import { ApplicationRef, WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { CatalogsApi } from '../api/catalogs.api';
import { AuthService } from '../auth/auth.service';
import { Specialty } from '../models/api.models';
import { SpecialtiesStore } from './specialties.store';

describe('SpecialtiesStore', () => {
  let store: SpecialtiesStore;
  let api: { specialties: ReturnType<typeof vi.fn> };
  let session: WritableSignal<{ clinicId: number } | null>;

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  beforeEach(() => {
    session = signal<{ clinicId: number } | null>({ clinicId: 1 });
    api = {
      specialties: vi.fn(() =>
        of<Specialty[]>([
          { id: 2, name: 'Pediatría' },
          { id: 1, name: 'Cardiología' },
        ]),
      ),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: CatalogsApi, useValue: api },
        { provide: AuthService, useValue: { session } },
      ],
    });
    store = TestBed.inject(SpecialtiesStore);
  });

  it('does not load until requested', async () => {
    await settle();
    expect(api.specialties).not.toHaveBeenCalled();
    expect(store.specialties()).toEqual([]);
  });

  it('loads lazily and sorts by name', async () => {
    store.load();
    await settle();
    expect(store.specialties().map((s) => s.name)).toEqual(['Cardiología', 'Pediatría']);
  });

  it('caches the list across load calls for the same clinic', async () => {
    store.load();
    await settle();
    store.load();
    await settle();
    expect(api.specialties).toHaveBeenCalledOnce();
  });

  it('reloads on demand', async () => {
    store.load();
    await settle();
    store.reload();
    await settle();
    expect(api.specialties).toHaveBeenCalledTimes(2);
  });

  it('retries on load after an error', async () => {
    api.specialties.mockReturnValueOnce(throwError(() => new Error('offline')));
    store.load();
    await settle();
    expect(store.errorMessage()).toBe('No se pudieron cargar las especialidades');

    store.load();
    await settle();
    expect(api.specialties).toHaveBeenCalledTimes(2);
    expect(store.errorMessage()).toBeNull();
  });

  it('drops the cache and waits for a new request when the clinic changes', async () => {
    store.load();
    await settle();

    session.set({ clinicId: 2 });
    await settle();
    expect(api.specialties).toHaveBeenCalledOnce();

    store.load();
    await settle();
    expect(api.specialties).toHaveBeenCalledTimes(2);
  });

  it('does not load without a session', async () => {
    session.set(null);
    store.load();
    await settle();
    expect(api.specialties).not.toHaveBeenCalled();
  });

  describe('upsert', () => {
    it('inserts or replaces a specialty locally keeping the order', async () => {
      store.load();
      await settle();

      store.upsert({ id: 3, name: 'Dermatología' });
      store.upsert({ id: 2, name: 'Alergología' });

      expect(store.specialties()).toEqual([
        { id: 2, name: 'Alergología' },
        { id: 1, name: 'Cardiología' },
        { id: 3, name: 'Dermatología' },
      ]);
      expect(api.specialties).toHaveBeenCalledOnce();
    });

    it('invalidates a load in flight instead of patching it', async () => {
      const first = new Subject<Specialty[]>();
      api.specialties.mockReturnValueOnce(first);
      store.load();
      TestBed.tick();

      store.upsert({ id: 3, name: 'Dermatología' });
      await settle();

      expect(api.specialties).toHaveBeenCalledTimes(2);
    });

    it('does nothing before the list was requested', async () => {
      store.upsert({ id: 3, name: 'Dermatología' });
      await settle();
      expect(api.specialties).not.toHaveBeenCalled();
    });
  });
});
