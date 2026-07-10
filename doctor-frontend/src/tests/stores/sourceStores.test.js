import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const apiMock = vi.hoisted(() => ({
  getDoctorInfo: vi.fn(),
  getRegistrationDetail: vi.fn(),
  getTodayRegistrations: vi.fn(),
  startConsultation: vi.fn()
}))

vi.mock('../../api/doctor', () => apiMock)

import { useDoctorStore } from '../../stores/doctorStore'
import { usePatientStore } from '../../stores/patientStore'
import { usePrescriptionStore } from '../../stores/prescriptionStore'
import { useRegistrationStore } from '../../stores/registrationStore'

describe('doctor Pinia stores from source', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    apiMock.getDoctorInfo.mockResolvedValue({ data: { id: 1, name: 'Alice', departmentName: '内科', title: '主任医师' } })
    apiMock.getRegistrationDetail.mockResolvedValue({ data: { id: 10, patientName: 'Bob' } })
    apiMock.getTodayRegistrations.mockResolvedValue({
      data: [
        { id: 1, patientName: 'Bob', status: 'waiting' },
        { id: 2, patientName: 'Alice', status: 'done' }
      ]
    })
    apiMock.startConsultation.mockResolvedValue({ data: 'ok' })
  })

  it('loads, caches, updates and clears doctor profile and notifications', async () => {
    const store = useDoctorStore()
    await store.fetchDoctorInfo()
    expect(store.doctorName).toBe('Alice')
    expect(store.doctorId).toBe(1)
    expect(store.departmentName).toBe('内科')

    store.updateDoctorInfo({ name: 'Ann' })
    expect(store.doctorInitial).toBe('A')
    store.addNotification({ title: 'critical' })
    store.addNotification({ title: 'other' })
    expect(store.unreadCount).toBe(2)
    store.markRead(0)
    store.markAllRead()
    expect(store.unreadCount).toBe(0)
    store.setWsConnected(true)
    expect(store.wsConnected).toBe(true)
    store.clearDoctorInfo()
    expect(store.doctorInfo).toBeNull()
  })

  it('covers doctor profile defaults, cache guards and notification limits', async () => {
    const store = useDoctorStore()
    expect(store.doctorName).toBe('医生')
    expect(store.doctorId).toBeNull()
    expect(store.doctorInitial).toBe('医')
    expect(store.departmentName).toBeTruthy()
    expect(store.doctorTitle).toBeTruthy()

    apiMock.getDoctorInfo.mockRejectedValueOnce(new Error('offline'))
    await store.fetchDoctorInfo()
    expect(store.doctorInfo).toBeNull()

    store.doctorInfo = { id: 4, name: 'Existing' }
    localStorage.setItem('doctorInfo', JSON.stringify({ id: 5, name: 'Cached' }))
    store.restoreFromCache()
    expect(store.doctorId).toBe(4)

    for (let i = 0; i < 51; i += 1) store.addNotification({ title: `n-${i}` })
    expect(store.notifications).toHaveLength(50)
    store.markRead(999)
    expect(store.notifications).toHaveLength(50)
  })

  it('restores doctor profile from cache when API fails', async () => {
    localStorage.setItem('doctorInfo', JSON.stringify({ id: 2, name: 'Cached' }))
    apiMock.getDoctorInfo.mockRejectedValueOnce(new Error('offline'))
    const store = useDoctorStore()
    await store.fetchDoctorInfo()
    expect(store.doctorName).toBe('Cached')
    store.clearDoctorInfo()
    localStorage.setItem('doctorInfo', JSON.stringify({ id: 3, name: 'Cache2' }))
    store.restoreFromCache()
    expect(store.doctorId).toBe(3)
  })

  it('caches patient details and updates current patient state', async () => {
    const store = usePatientStore()
    const first = await store.fetchPatient(10)
    const second = await store.fetchPatient(10)
    expect(first).toEqual(second)
    expect(apiMock.getRegistrationDetail).toHaveBeenCalledTimes(1)
    store.setCurrentPatient(10)
    expect(store.currentPatient?.patientName).toBe('Bob')
    store.updateCachedPatient(10, { patientName: 'Ben' })
    expect(store.currentPatient?.patientName).toBe('Ben')
    store.clearCache()
    expect(store.currentPatient).toBeNull()
  })

  it('covers patient cache misses and missing current patients', async () => {
    const store = usePatientStore()
    expect(store.currentPatient).toBeNull()
    store.setCurrentPatient(999)
    expect(store.currentPatient).toBeNull()
    store.updateCachedPatient(999, { patientName: 'Nobody' })
    expect(store.patientCache.has(999)).toBe(false)
  })

  it('manages prescription drafts, review cache and submit history', () => {
    const store = usePrescriptionStore()
    store.initDraft(1, { diagnosis: 'cold' })
    expect(store.activeDraft.diagnosis).toBe('cold')
    store.updateDraft(1, { notes: 'rest' })
    expect(store.activeDraft.notes).toBe('rest')
    store.setReviewing(true)
    store.setReviewResult(1, { reviewResult: 'pass' })
    expect(store.getReviewResult(1).reviewResult).toBe('pass')
    store.setSubmitting(true)
    store.recordSubmit(1, 99)
    expect(store.todaySubmitCount).toBe(1)
    expect(store.activeDraft).toBeNull()
    store.clearAll()
    expect(store.submitHistory).toHaveLength(0)
  })

  it('covers prescription guard branches and cached review misses', () => {
    const store = usePrescriptionStore()
    expect(store.activeDraft).toBeNull()
    expect(store.getReviewResult(404)).toBeNull()

    store.updateDraft(404, { diagnosis: 'missing' })
    expect(store.drafts.has(404)).toBe(false)

    store.initDraft(1, { diagnosis: 'first' })
    store.initDraft(1, { diagnosis: 'second' })
    expect(store.activeDraft.diagnosis).toBe('first')

    store.initDraft(2)
    store.clearDraft(1)
    expect(store.activeRegistrationId).toBe(2)
    store.clearReviewResult(404)
    store.setReviewing(false)
    store.setSubmitting(false)
    expect(store.isReviewing).toBe(false)
    expect(store.isSubmitting).toBe(false)
  })

  it('loads and filters registration queue', async () => {
    const store = useRegistrationStore()
    await store.loadTodayRegistrations()
    expect(store.totalCount).toBe(2)
    store.setKeyword('Bob')
    expect(store.filteredList).toHaveLength(1)
    store.setStatusFilter('waiting')
    expect(store.filteredList).toHaveLength(1)
    await store.startRegistration(1)
    expect(store.currentRegistration?.id).toBe(1)
    store.markDone(1)
    store.setCurrentRegistration(2)
    expect(store.currentRegistration?.id).toBe(2)
    store.resetFilter()
    store.clearAll()
    expect(store.totalCount).toBe(0)
  })

  it('covers registration empty filters, status counts and missing items', async () => {
    apiMock.getTodayRegistrations.mockResolvedValueOnce({
      data: [
        { id: 1, patientName: 'Bob', status: '待就诊' },
        { id: 2, patientName: 'Alice', status: '就诊中' },
        { id: 3, status: '已就诊' },
        { id: 4, patientName: 'Eve', status: '已取消' }
      ]
    })
    const store = useRegistrationStore()
    expect(store.currentRegistration).toBeNull()
    await store.loadTodayRegistrations()
    expect(store.waitingCount).toBe(1)
    expect(store.activeCount).toBe(1)
    expect(store.doneCount).toBe(1)
    expect(store.filteredList).toHaveLength(4)

    store.setKeyword('   ')
    expect(store.filteredList).toHaveLength(3)
    store.setKeyword('NoMatch')
    expect(store.filteredList).toHaveLength(0)
    store.setKeyword('')
    store.setStatusFilter('missing')
    expect(store.filteredList).toHaveLength(0)

    await store.startRegistration(999)
    expect(store.currentRegistration).toBeNull()
    store.markDone(999)
    store.setCurrentRegistration(999)
    expect(store.currentRegistration).toBeNull()
  })
})
