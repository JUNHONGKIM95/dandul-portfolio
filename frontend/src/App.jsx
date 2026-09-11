import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Cropper from 'react-easy-crop'
import {
  CalendarDays,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Clock,
  Film,
  Heart,
  Home,
  Image as ImageIcon,
  MapPin,
  Mountain,
  Pencil,
  Plus,
  RotateCcw,
  RotateCw,
  Search,
  Trash2,
  Utensils,
  Upload,
  X,
} from 'lucide-react'
import Waves from './components/Waves'
import { bacMountains, findBacMountainForRecord } from './data/bacMountains'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''
const KAKAO_MAPS_API_KEY = import.meta.env.VITE_KAKAO_MAPS_API_KEY || ''
const START_DATE = '2024-03-30'
const JUNHONG_BIRTHDAY = { label: '준홍 생일', month: 2, day: 8 }
const SOMIN_BIRTHDAY = { label: '소민 생일', month: 8, day: 31 }
const AUTH_STORAGE_KEY = 'jhsm-current-user'
const SPLASH_STORAGE_KEY = 'dandul-splash-photo-url'
const DEFAULT_SPLASH_PHOTO_URL = '/dandul-pwa-splash-v4.svg'
const SPLASH_ASPECT = 9 / 19.5
const PUSH_STORAGE_PREFIX = 'dandul-push-enabled'
const PUSH_DISABLED_PREFIX = 'dandul-push-disabled'
const VISIT_STORAGE_PREFIX = 'dandul-visit-recorded'
const UNKNOWN_PLACE_LABEL = '장소 미정'
const MAX_UPLOAD_IMAGE_DIMENSION = 2400
const KOREA_TIME_ZONE = 'Asia/Seoul'
const DEFAULT_AUTHOR = { username: 'junhong', nickname: '준홍' }
const EVENT_CATEGORIES = [
  { value: 'junhong', label: '준홍' },
  { value: 'somin', label: '소민' },
  { value: 'together', label: '같이' },
]
const FOODIE_CATEGORY_FILTERS = [
  { value: 'all', label: '전체' },
  { value: '한식', label: '한식' },
  { value: '중식', label: '중식' },
  { value: '일식', label: '일식' },
  { value: '양식', label: '양식' },
  { value: '카페', label: '카페' },
  { value: '분식', label: '분식' },
  { value: '술집', label: '술집' },
]
const KOREAN_HOLIDAYS = {
  '2024-01-01': '신정',
  '2024-02-09': '설날',
  '2024-02-10': '설날',
  '2024-02-11': '설날',
  '2024-02-12': '설날 대체공휴일',
  '2024-03-01': '삼일절',
  '2024-04-10': '국회의원 선거일',
  '2024-05-05': '어린이날',
  '2024-05-06': '어린이날 대체공휴일',
  '2024-05-15': '부처님오신날',
  '2024-06-06': '현충일',
  '2024-08-15': '광복절',
  '2024-09-16': '추석',
  '2024-09-17': '추석',
  '2024-09-18': '추석',
  '2024-10-03': '개천절',
  '2024-10-09': '한글날',
  '2024-12-25': '성탄절',
  '2025-01-01': '신정',
  '2025-01-27': '임시공휴일',
  '2025-01-28': '설날',
  '2025-01-29': '설날',
  '2025-01-30': '설날',
  '2025-03-01': '삼일절',
  '2025-03-03': '삼일절 대체공휴일',
  '2025-05-05': '어린이날 · 부처님오신날',
  '2025-05-06': '부처님오신날 대체공휴일',
  '2025-06-03': '대통령 선거일',
  '2025-06-06': '현충일',
  '2025-08-15': '광복절',
  '2025-10-03': '개천절',
  '2025-10-05': '추석',
  '2025-10-06': '추석',
  '2025-10-07': '추석',
  '2025-10-08': '추석 대체공휴일',
  '2025-10-09': '한글날',
  '2025-12-25': '성탄절',
  '2026-01-01': '신정',
  '2026-02-16': '설날',
  '2026-02-17': '설날',
  '2026-02-18': '설날',
  '2026-03-01': '삼일절',
  '2026-03-02': '삼일절 대체공휴일',
  '2026-05-05': '어린이날',
  '2026-05-24': '부처님오신날',
  '2026-05-25': '부처님오신날 대체공휴일',
  '2026-06-03': '지방선거일',
  '2026-06-06': '현충일',
  '2026-08-15': '광복절',
  '2026-08-17': '광복절 대체공휴일',
  '2026-09-24': '추석',
  '2026-09-25': '추석',
  '2026-09-26': '추석',
  '2026-10-03': '개천절',
  '2026-10-05': '개천절 대체공휴일',
  '2026-10-09': '한글날',
  '2026-12-25': '성탄절',
}

const navItems = [
  { id: 'home', label: 'HOME', icon: Home },
  { id: 'calendar', label: 'CALENDAR', icon: CalendarDays },
  { id: 'album', label: 'ALBUM', icon: ImageIcon },
  { id: 'hiking', label: 'HIKING', icon: Mountain },
]

function formatDate(date) {
  if (!date) return ''
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(`${date}T00:00:00`))
}

function formatDateWithWeekday(date) {
  if (!date) return ''
  const target = new Date(`${date}T00:00:00`)
  const baseDate = new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(target)
  const weekday = new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(target)
  return `${baseDate} (${weekday})`
}

function formatDatePlainWeekday(date) {
  if (!date) return ''
  const target = new Date(`${date}T00:00:00`)
  const baseDate = new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(target)
  const weekday = new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(target)
  return `${baseDate} ${weekday}`
}

function formatDateTime(value) {
  if (!value) return ''
  const rawValue = String(value)
  const hasTimeZone = /(?:Z|[+-]\d{2}:?\d{2})$/.test(rawValue)
  const target = new Date(hasTimeZone ? rawValue : `${rawValue}+09:00`)
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIME_ZONE,
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(target)
}

function formatMonth(date) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
  }).format(date)
}

function toDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function emptyDateParts() {
  return { year: '', month: '', day: '' }
}

function datePartsToInput(parts) {
  if (!parts.year || !parts.month || !parts.day) return ''
  return `${parts.year}-${parts.month.padStart(2, '0')}-${parts.day.padStart(2, '0')}`
}

function HikingDateSelect({ label, value, years, onChange }) {
  const dayCount =
    value.year && value.month
      ? new Date(Number(value.year), Number(value.month), 0).getDate()
      : 31

  return (
    <div className="hiking-date-select">
      <span>{label}</span>
      <div className="hiking-date-parts">
        <select
          value={value.year}
          aria-label={`${label} 연도`}
          onChange={(event) => onChange({ year: event.target.value, month: '', day: '' })}
        >
          <option value="">연도</option>
          {years.map((year) => (
            <option key={year} value={String(year)}>
              {year}년
            </option>
          ))}
        </select>
        <select
          value={value.month}
          aria-label={`${label} 월`}
          disabled={!value.year}
          onChange={(event) => onChange({ ...value, month: event.target.value, day: '' })}
        >
          <option value="">월</option>
          {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
            <option key={month} value={String(month)}>
              {month}월
            </option>
          ))}
        </select>
        <select
          value={value.day}
          aria-label={`${label} 일`}
          disabled={!value.year || !value.month}
          onChange={(event) => onChange({ ...value, day: event.target.value })}
        >
          <option value="">일</option>
          {Array.from({ length: dayCount }, (_, index) => index + 1).map((day) => (
            <option key={day} value={String(day)}>
              {day}일
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}

function todayInKorea() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: KOREA_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  return new Date(Number(values.year), Number(values.month) - 1, Number(values.day))
}

function fromDateInput(value) {
  return new Date(`${value}T00:00:00`)
}

function eventEndDate(event) {
  return event?.endDate || event?.date
}

function dateRangeKeys(startDate, endDate = startDate) {
  if (!startDate) return []
  const start = fromDateInput(startDate)
  const end = fromDateInput(endDate || startDate)
  const normalizedEnd = end < start ? start : end
  const keys = []
  for (
    let cursor = new Date(start);
    cursor <= normalizedEnd;
    cursor.setDate(cursor.getDate() + 1)
  ) {
    keys.push(toDateInput(cursor))
  }
  return keys
}

function isDateInEventRange(dateKey, event) {
  return dateKey >= event.date && dateKey <= eventEndDate(event)
}

function formatEventDateRange(event) {
  if (!event?.endDate || event.endDate === event.date) return formatDate(event?.date)
  return `${formatDate(event.date)} - ${formatDate(event.endDate)}`
}

function startOfToday() {
  const today = todayInKorea()
  today.setHours(0, 0, 0, 0)
  return today
}

function daysBetween(start, end) {
  const millisecondsPerDay = 24 * 60 * 60 * 1000
  return Math.round((end.getTime() - start.getTime()) / millisecondsPerDay)
}

function nextAnnualDate(month, day, today = startOfToday()) {
  let target = new Date(today.getFullYear(), month - 1, day)
  if (target < today) {
    target = new Date(today.getFullYear() + 1, month - 1, day)
  }
  return target
}

function relationshipMilestoneDate(dayCount) {
  const target = fromDateInput(START_DATE)
  target.setDate(target.getDate() + dayCount - 1)
  return target
}

function ddayLabel(daysUntil) {
  return daysUntil === 0 ? 'D-day' : `D-${daysUntil}`
}

function upcomingAnniversaries(daysTogether) {
  const today = startOfToday()
  const firstMilestone = Math.max(800, Math.ceil(Math.max(daysTogether, 1) / 100) * 100)
  const milestones = Array.from({ length: 6 }, (_, index) => {
    const dayCount = firstMilestone + index * 100
    const date = relationshipMilestoneDate(dayCount)
    return {
      id: `love-${dayCount}`,
      title: `${dayCount.toLocaleString('ko-KR')}일`,
      date,
      daysUntil: daysBetween(today, date),
      type: '연애 기념일',
    }
  })
  const birthdays = [JUNHONG_BIRTHDAY, SOMIN_BIRTHDAY].map((birthday) => {
    const date = nextAnnualDate(birthday.month, birthday.day, today)
    return {
      id: birthday.label,
      title: birthday.label,
      date,
      daysUntil: daysBetween(today, date),
      type: '생일',
    }
  })

  return [...birthdays, ...milestones]
    .filter((item) => item.daysUntil >= 0)
    .sort((first, second) => first.daysUntil - second.daysUntil || first.date - second.date)
}

function parseCoordinate(value, min, max) {
  if (value === '' || value === null || value === undefined) return null
  const coordinate = Number(value)
  return Number.isFinite(coordinate) && coordinate >= min && coordinate <= max ? coordinate : null
}

function decimalPlaces(value) {
  const text = String(value)
  if (!text.includes('.')) return 0
  return text.split('.')[1].replace(/0+$/, '').length
}

function hasValidCoordinates(record) {
  return (
    parseCoordinate(record.latitude, -90, 90) !== null &&
    parseCoordinate(record.longitude, -180, 180) !== null
  )
}

function isLikelyLegacyEstimatedCoordinate(record) {
  return hasValidCoordinates(record) && decimalPlaces(record.latitude) <= 4 && decimalPlaces(record.longitude) <= 4
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => {
    const entities = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }
    return entities[character]
  })
}

function normalizeMountainName(value) {
  return String(value || '').replace(/\s+/g, '').trim().toLowerCase()
}

function authorNickname(item) {
  return item?.creatorNickname || DEFAULT_AUTHOR.nickname
}

function authorUsername(item) {
  return item?.createdBy || DEFAULT_AUTHOR.username
}

function authorBadgeClass(item) {
  return `author-badge author-badge-${authorUsername(item)}`
}

function isBacRecord(record) {
  if (record?.source === 'custom') return false
  if (record?.source === 'bac') return true
  return Boolean(findBacMountainForRecord(record))
}

function timeLabel(value) {
  return value ? value.slice(0, 5) : '시간 미정'
}

function eventCategory(value) {
  return EVENT_CATEGORIES.some((category) => category.value === value) ? value : 'together'
}

function eventCategoryLabel(value) {
  return EVENT_CATEGORIES.find((category) => category.value === eventCategory(value))?.label || '같이'
}

function isWeekendDateKey(dateKey) {
  const day = fromDateInput(dateKey).getDay()
  return day === 0 || day === 6
}

function holidayEvent(dateKey) {
  const title = KOREAN_HOLIDAYS[dateKey]
  if (!title || isWeekendDateKey(dateKey)) return null
  return {
    id: `holiday-${dateKey}`,
    title,
    date: dateKey,
    endDate: dateKey,
    meetingTime: null,
    place: '대한민국 공휴일',
    memo: '',
    category: 'holiday',
    isHoliday: true,
    createdBy: 'holiday',
    creatorNickname: '공휴일',
  }
}

function calendarEventCategory(event) {
  return event?.isHoliday ? 'holiday' : eventCategory(event?.category)
}

function calendarEventCategoryLabel(event) {
  return event?.isHoliday ? '공휴일' : eventCategoryLabel(event?.category)
}

function normalizeMediaUrl(url) {
  if (!url) return ''
  try {
    const parsedUrl = new URL(url)
    if (parsedUrl.pathname.startsWith('/uploads/')) {
      return `${parsedUrl.pathname}${parsedUrl.search}`
    }
  } catch {
    // Relative URLs are already safe to use as-is.
  }
  return url
}

function withCacheKey(url, key) {
  const safeUrl = normalizeMediaUrl(url)
  if (!safeUrl) return ''
  return `${safeUrl}${safeUrl.includes('?') ? '&' : '?'}v=${encodeURIComponent(key)}`
}

function mediaCacheKey(item) {
  return `${item.id}-${item.updatedAt || ''}`
}

function mediaSortDate(item) {
  return item.capturedAt || item.eventDate || ''
}

function compareMediaByDateDesc(first, second) {
  const dateCompare = mediaSortDate(second).localeCompare(mediaSortDate(first))
  if (dateCompare !== 0) return dateCompare
  return (second.updatedAt || '').localeCompare(first.updatedAt || '')
}

function eventDistanceFromDate(event, baseDate) {
  const start = fromDateInput(event.date)
  const end = fromDateInput(eventEndDate(event))
  if (baseDate >= start && baseDate <= end) return 0
  return Math.min(Math.abs(daysBetween(baseDate, start)), Math.abs(daysBetween(baseDate, end)))
}

function compareEventsByClosestDate(first, second, baseDate = todayInKorea()) {
  const distanceCompare = eventDistanceFromDate(first, baseDate) - eventDistanceFromDate(second, baseDate)
  if (distanceCompare !== 0) return distanceCompare

  const firstStartDelta = daysBetween(baseDate, fromDateInput(first.date))
  const secondStartDelta = daysBetween(baseDate, fromDateInput(second.date))
  if (firstStartDelta >= 0 && secondStartDelta < 0) return -1
  if (firstStartDelta < 0 && secondStartDelta >= 0) return 1

  const dateCompare = first.date.localeCompare(second.date)
  if (dateCompare !== 0) return dateCompare
  return first.title.localeCompare(second.title)
}

function compareEventsByDateDesc(first, second) {
  const dateCompare = second.date.localeCompare(first.date)
  if (dateCompare !== 0) return dateCompare
  return first.title.localeCompare(second.title)
}

let googleMapsLoader

function loadGoogleMaps(apiKey) {
  if (window.google?.maps) return Promise.resolve(window.google.maps)
  if (!apiKey) return Promise.reject(new Error('Google Maps API key is missing.'))
  if (googleMapsLoader) return googleMapsLoader

  googleMapsLoader = new Promise((resolve, reject) => {
    const callbackName = `initGoogleMaps${Date.now()}`
    window[callbackName] = () => {
      resolve(window.google.maps)
      delete window[callbackName]
    }

    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&callback=${callbackName}`
    script.async = true
    script.defer = true
    script.onerror = () => {
      delete window[callbackName]
      reject(new Error('Google Maps script failed to load.'))
    }
    document.head.appendChild(script)
  })

  return googleMapsLoader
}

let kakaoMapsLoader

function loadKakaoMaps(apiKey) {
  if (window.kakao?.maps?.services) return Promise.resolve(window.kakao.maps)
  if (!apiKey) return Promise.reject(new Error('Kakao Maps API key is missing.'))
  if (kakaoMapsLoader) return kakaoMapsLoader

  kakaoMapsLoader = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(apiKey)}&autoload=false&libraries=services`
    script.async = true
    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao.maps))
    }
    script.onerror = () => reject(new Error('Kakao Maps script failed to load.'))
    document.head.appendChild(script)
  })

  return kakaoMapsLoader
}

function kakaoPlaceToFoodieForm(place, currentUser = DEFAULT_AUTHOR) {
  return {
    id: null,
    kakaoPlaceId: place.id || '',
    name: place.place_name || '',
    categoryName: place.category_name || '',
    addressName: place.address_name || '',
    roadAddressName: place.road_address_name || '',
    phone: place.phone || '',
    placeUrl: place.place_url || '',
    latitude: place.y || '',
    longitude: place.x || '',
    kakaoRating: place.rating || '',
    kakaoReviewCount: place.review_count || '',
    eventId: '',
    visitDate: toDateInput(todayInKorea()),
    oneLineReview: '',
    junhongRating: '0',
    sominRating: '0',
    junhongReview: '',
    sominReview: '',
    photoFile: null,
    createdBy: currentUser.username,
    creatorNickname: currentUser.nickname,
  }
}

function userRatingLabel(value) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(1) : '-'
}

function matchesFoodieFilter(place, filter) {
  if (!filter || filter === 'all') return true
  return (place.category_name || place.categoryName || '').includes(filter)
}

function filterFoodiePlacesByCategory(places, filter) {
  return places.filter((place) => matchesFoodieFilter(place, filter))
}

function kakaoDistanceFrom(point, place) {
  const lat = Number(place.y)
  const lng = Number(place.x)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return Number.POSITIVE_INFINITY
  const dx = (lng - point.lng) * Math.cos(((lat + point.lat) / 2) * (Math.PI / 180))
  const dy = lat - point.lat
  return Math.sqrt(dx * dx + dy * dy)
}

function createKakaoMarkerImage(maps, color) {
  const svg = encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 32 42">
      <path fill="${color}" stroke="white" stroke-width="2" d="M16 1C7.7 1 1 7.7 1 16c0 11.3 15 25 15 25s15-13.7 15-25C31 7.7 24.3 1 16 1z"/>
      <circle cx="16" cy="16" r="6.2" fill="white" opacity=".95"/>
    </svg>
  `)
  return new maps.MarkerImage(
    `data:image/svg+xml;charset=UTF-8,${svg}`,
    new maps.Size(32, 42),
    { offset: new maps.Point(16, 40) },
  )
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || '요청을 처리하지 못했어요.')
  }
  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

const mediaCommentsCache = new Map()
let mediaCommentsCacheLoaded = false
let mediaCommentsCachePromise = null

function cacheMediaComments(comments) {
  mediaCommentsCache.clear()
  comments.forEach((comment) => {
    const mediaComments = mediaCommentsCache.get(comment.mediaItemId) || []
    mediaComments.push(comment)
    mediaCommentsCache.set(comment.mediaItemId, mediaComments)
  })
  mediaCommentsCacheLoaded = true
}

function preloadMediaComments() {
  if (mediaCommentsCacheLoaded) return Promise.resolve(mediaCommentsCache)
  if (!mediaCommentsCachePromise) {
    mediaCommentsCachePromise = request('/api/media/comments')
      .then((comments) => {
        cacheMediaComments(comments)
        return mediaCommentsCache
      })
      .catch((error) => {
        mediaCommentsCachePromise = null
        throw error
      })
  }
  return mediaCommentsCachePromise
}

function isPushSupported() {
  return (
    typeof window !== 'undefined' &&
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  )
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)

  for (let index = 0; index < rawData.length; index += 1) {
    outputArray[index] = rawData.charCodeAt(index)
  }

  return outputArray
}

function arrayBufferToBase64Url(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function subscriptionMatchesPublicKey(subscription, publicKey) {
  const subscriptionKey = subscription?.options?.applicationServerKey
  if (!subscriptionKey) return true
  return arrayBufferToBase64Url(subscriptionKey) === publicKey.replace(/=+$/, '')
}

async function ensurePushSubscription(registration, publicKey, { forceNew = false } = {}) {
  const existingSubscription = await registration.pushManager.getSubscription()
  if (!forceNew && existingSubscription && subscriptionMatchesPublicKey(existingSubscription, publicKey)) {
    return existingSubscription
  }
  if (existingSubscription) {
    await existingSubscription.unsubscribe()
  }
  return registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(publicKey),
  })
}

async function registerPushServiceWorker() {
  const registration = await navigator.serviceWorker.register('/push-sw.js', { updateViaCache: 'none' })
  await registration.update().catch(() => {})
  return navigator.serviceWorker.ready
}

function pushStorageKey(username) {
  return `${PUSH_STORAGE_PREFIX}-${username || 'guest'}`
}

function pushDisabledKey(username) {
  return `${PUSH_DISABLED_PREFIX}-${username || 'guest'}`
}

function visitStorageKey(username) {
  return `${VISIT_STORAGE_PREFIX}-${username || 'guest'}`
}

function isAdminUser(user) {
  return user?.username === 'admin' || user?.role === 'ADMIN'
}

function createEmptyEventForm(date = toDateInput(new Date())) {
  return {
    id: null,
    title: '',
    date,
    endDate: date,
    isRange: false,
    meetingTime: '',
    timeUnknown: false,
    place: '',
    placeUnknown: false,
    memo: '',
    category: 'together',
  }
}

function createEmptyMediaForm() {
  return {
    mediaType: 'PHOTO',
    eventId: '',
    title: '',
    memo: '',
    capturedAt: '',
    rotationDegrees: 0,
    file: null,
  }
}

function isBrowserNormalizablePhoto(file) {
  if (!file) return false
  const type = file.type.toLowerCase()
  const name = file.name.toLowerCase()
  if (type === 'image/gif' || type === 'image/svg+xml' || name.endsWith('.gif') || name.endsWith('.svg')) {
    return false
  }
  return type.startsWith('image/') || /\.(jpe?g|png|webp|bmp)$/i.test(name)
}

function blobToImage(blob) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(blob)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('사진 파일을 읽지 못했어요.'))
    }
    image.src = url
  })
}

async function normalizePhotoForUpload(file) {
  if (!isBrowserNormalizablePhoto(file)) return file

  const image = await blobToImage(file)
  const scale = Math.min(1, MAX_UPLOAD_IMAGE_DIMENSION / Math.max(image.naturalWidth, image.naturalHeight))
  const width = Math.max(1, Math.round(image.naturalWidth * scale))
  const height = Math.max(1, Math.round(image.naturalHeight * scale))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.drawImage(image, 0, 0, width, height)

  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (result) => (result ? resolve(result) : reject(new Error('사진 변환에 실패했어요.'))),
      'image/jpeg',
      0.92,
    )
  })
  const baseName = file.name.replace(/\.[^.]+$/, '') || 'photo'
  return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg', lastModified: file.lastModified })
}

function App() {
  const [activeView, setActiveView] = useState('home')
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem(AUTH_STORAGE_KEY)
    return saved ? JSON.parse(saved) : null
  })
  const [showOnboarding, setShowOnboarding] = useState(() => !localStorage.getItem(AUTH_STORAGE_KEY))
  const [showAppSplash, setShowAppSplash] = useState(true)
  const [notificationStatus, setNotificationStatus] = useState(() => {
    if (!isPushSupported()) return 'unsupported'
    return Notification.permission === 'granted' ? 'ready' : Notification.permission
  })
  const [pushSubscriptionCount, setPushSubscriptionCount] = useState(null)
  const [pendingMediaId, setPendingMediaId] = useState(() => {
    const params = new URLSearchParams(window.location.search)
    return params.get('mediaId')
  })
  const [profile, setProfile] = useState(null)
  const [splashPhotoUrl, setSplashPhotoUrl] = useState(
    () => localStorage.getItem(SPLASH_STORAGE_KEY) || DEFAULT_SPLASH_PHOTO_URL,
  )
  const [events, setEvents] = useState([])
  const [media, setMedia] = useState([])
  const [hikingRecords, setHikingRecords] = useState([])
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const workspaceRef = useRef(null)

  useEffect(() => {
    const timer = window.setTimeout(() => setShowAppSplash(false), 1800)
    return () => window.clearTimeout(timer)
  }, [])

  const applyProfileData = useCallback((profileData) => {
    setProfile(profileData)
    const nextSplashPhotoUrl = profileData.splashPhotoUrl || DEFAULT_SPLASH_PHOTO_URL
    setSplashPhotoUrl(nextSplashPhotoUrl)
    if (profileData.splashPhotoUrl) {
      localStorage.setItem(SPLASH_STORAGE_KEY, profileData.splashPhotoUrl)
    } else {
      localStorage.removeItem(SPLASH_STORAGE_KEY)
    }
  }, [])

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const view = params.get('view')
    if (view && navItems.some((item) => item.id === view)) {
      setActiveView(view)
    }
  }, [])

  const refreshAll = useCallback(async () => {
    setError('')
    try {
      const [profileData, eventData, mediaData, hikingData] = await Promise.all([
        request('/api/profile'),
        request('/api/events'),
        request('/api/media'),
        request('/api/hiking-records'),
      ])
      applyProfileData(profileData)
      setEvents(eventData)
      setMedia(mediaData)
      setHikingRecords(hikingData)
    } catch (err) {
      setError(err.message)
    }
  }, [applyProfileData])

  useEffect(() => {
    let cancelled = false

    async function loadInitialData() {
      setError('')
      try {
        const [profileData, eventData, mediaData, hikingData] = await Promise.all([
          request('/api/profile'),
          request('/api/events'),
          request('/api/media'),
          request('/api/hiking-records'),
        ])
        if (!cancelled) {
          applyProfileData(profileData)
          setEvents(eventData)
          setMedia(mediaData)
          setHikingRecords(hikingData)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }

    loadInitialData()
    return () => {
      cancelled = true
    }
  }, [applyProfileData])

  const runAction = async (message, action) => {
    setError('')
    setStatus('')
    try {
      await action()
      await refreshAll()
      setStatus(message)
      return true
    } catch (err) {
      setError(err.message)
      return false
    }
  }

  const favoriteMedia = media.filter((item) => item.favorite)

  const login = async (credentials) => {
    const user = await request('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    })
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user))
    setCurrentUser(user)
    setPushSubscriptionCount(null)
  }

  const registerPushSubscription = async (subscription) => {
    const json = subscription.toJSON()
    await request('/api/push/subscriptions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: currentUser.username,
        nickname: currentUser.nickname,
        endpoint: json.endpoint,
        keys: json.keys,
      }),
    })
    const status = await request(`/api/push/subscriptions/status?username=${encodeURIComponent(currentUser.username)}`)
    if (!status.enabled || status.subscriptionCount < 1) {
      throw new Error('알림 구독이 서버에 저장되지 않았어요. 알림을 다시 켜주세요.')
    }
    setPushSubscriptionCount(status.subscriptionCount)
    return status
  }

  const logout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    setCurrentUser(null)
    setPushSubscriptionCount(null)
    setShowOnboarding(true)
    setStatus('')
    setError('')
  }

  useEffect(() => {
    if (!currentUser || isAdminUser(currentUser) || !isPushSupported()) return
    let cancelled = false

    async function syncPushState() {
      if (Notification.permission !== 'granted') {
        localStorage.removeItem(pushStorageKey(currentUser.username))
        if (!cancelled) setNotificationStatus(Notification.permission)
        return
      }

      try {
        const config = await request('/api/push/public-key')
        if (!config.enabled || !config.publicKey) {
          if (!cancelled) setNotificationStatus('unconfigured')
          return
        }

        const registration = await registerPushServiceWorker()
        const existingSubscription = await registration.pushManager.getSubscription()
        const savedAsEnabled = localStorage.getItem(pushStorageKey(currentUser.username)) === 'true'
        const serverStatus = await request(
          `/api/push/subscriptions/status?username=${encodeURIComponent(currentUser.username)}`,
        )
        const explicitlyDisabled = localStorage.getItem(pushDisabledKey(currentUser.username)) === 'true'
        const shouldRestore =
          Boolean(existingSubscription) ||
          savedAsEnabled ||
          serverStatus.enabled ||
          (!explicitlyDisabled && Notification.permission === 'granted')
        const shouldForceNewSubscription = Boolean(existingSubscription) && !serverStatus.enabled

        if (!cancelled) {
          setPushSubscriptionCount(serverStatus.subscriptionCount ?? 0)
        }

        if (explicitlyDisabled && !existingSubscription && !savedAsEnabled && !serverStatus.enabled) {
          if (!cancelled) setNotificationStatus('default')
          return
        }

        if (shouldRestore) {
          const subscription = await ensurePushSubscription(registration, config.publicKey, {
            forceNew: shouldForceNewSubscription,
          })
          localStorage.setItem(pushStorageKey(currentUser.username), 'true')
          const registeredStatus = await registerPushSubscription(subscription)
          if (!cancelled) {
            setNotificationStatus('subscribed')
            setPushSubscriptionCount(registeredStatus.subscriptionCount ?? 1)
          }
          return
        }

        if (!cancelled) setNotificationStatus('default')
      } catch {
        if (!cancelled) {
          setNotificationStatus('default')
          setPushSubscriptionCount(null)
        }
      }
    }

    syncPushState()
    return () => {
      cancelled = true
    }
  }, [currentUser])

  useEffect(() => {
    if (!currentUser || isAdminUser(currentUser)) return
    const key = visitStorageKey(currentUser.username)
    if (sessionStorage.getItem(key)) return
    sessionStorage.setItem(key, 'true')
    request('/api/visits', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: currentUser.username,
        nickname: currentUser.nickname,
      }),
    }).catch(() => {
      sessionStorage.removeItem(key)
    })
  }, [currentUser])

  const disablePushNotifications = async () => {
    const registration = await navigator.serviceWorker.getRegistration()
    const subscription = await registration?.pushManager.getSubscription()
    if (subscription) {
      const json = subscription.toJSON()
      await request('/api/push/subscriptions', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ endpoint: json.endpoint }),
      })
      await subscription.unsubscribe()
    }
    localStorage.setItem(pushDisabledKey(currentUser.username), 'true')
    localStorage.removeItem(pushStorageKey(currentUser.username))
    setPushSubscriptionCount(0)
    setNotificationStatus('default')
    setStatus('댓글 알림을 껐어요.')
  }

  const togglePushNotifications = async () => {
    setError('')
    setStatus('')

    if (!isPushSupported()) {
      setNotificationStatus('unsupported')
      setError('이 브라우저에서는 알림을 지원하지 않아요.')
      return
    }

    try {
      if (notificationStatus === 'subscribed' && pushSubscriptionCount > 0) {
        await disablePushNotifications()
        return
      }

      const config = await request('/api/push/public-key')
      if (!config.enabled || !config.publicKey) {
        setNotificationStatus('unconfigured')
        setError('알림 서버 키가 아직 설정되지 않았어요.')
        return
      }

      const permission = await Notification.requestPermission()
      setNotificationStatus(permission)
      if (permission !== 'granted') {
        setError('스마트폰 설정에서 DANDUL 알림을 허용해야 해요.')
        return
      }

      localStorage.removeItem(pushDisabledKey(currentUser.username))
      const registration = await registerPushServiceWorker()
      const subscription = await ensurePushSubscription(registration, config.publicKey, {
        forceNew: notificationStatus === 'subscribed' && !(pushSubscriptionCount > 0),
      })
      await registerPushSubscription(subscription)

      localStorage.setItem(pushStorageKey(currentUser.username), 'true')
      setNotificationStatus('subscribed')
      setStatus('댓글 알림을 받을 수 있어요.')
    } catch (err) {
      setError(err.message)
    }
  }

  const moveToView = (viewId) => {
    setActiveView(viewId)
    requestAnimationFrame(() => {
      workspaceRef.current?.scrollTo({ top: 0, left: 0, behavior: 'auto' })
      window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
      document.scrollingElement?.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    })
  }

  if (!currentUser) {
    if (showOnboarding) {
      return (
        <>
          <OnboardingView onStart={() => setShowOnboarding(false)} />
          <AppSplash active={showAppSplash} photoUrl={splashPhotoUrl} />
        </>
      )
    }
    return (
      <>
        <LoginView onLogin={login} />
        <AppSplash active={showAppSplash} photoUrl={splashPhotoUrl} />
      </>
    )
  }

  if (isAdminUser(currentUser)) {
    return (
      <>
        <AdminDashboard currentUser={currentUser} onLogout={logout} />
        <AppSplash active={showAppSplash} photoUrl={splashPhotoUrl} />
      </>
    )
  }

  return (
    <>
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-copy">
            <strong className="brand-name">DANDUL</strong>
          </div>
          <div className="brand-actions">
            <span className={`user-badge user-badge-${currentUser.username}`}>
              {currentUser.nickname}
            </span>
            <button
              type="button"
              className="brand-notification"
              onClick={togglePushNotifications}
              disabled={notificationStatus === 'unsupported'}
            >
              {notificationStatus === 'subscribed'
                ? pushSubscriptionCount > 0
                  ? '알림 ON'
                  : '알림 복구'
                : '알림 켜기'}
            </button>
            <button type="button" className="brand-logout" onClick={logout}>
              로그아웃
            </button>
          </div>
        </div>

        <nav className="nav-list" aria-label="주요 화면">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <button
                key={item.id}
                type="button"
                className={activeView === item.id ? 'nav-item active' : 'nav-item'}
                onClick={() => moveToView(item.id)}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </button>
            )
          })}
        </nav>
      </aside>

      <main className="workspace" ref={workspaceRef}>
        <header className="topbar">
          <div>
            <h1>{navItems.find((item) => item.id === activeView)?.label}</h1>
          </div>
          {activeView === 'home' && (
            <SplashPhotoControl onAction={runAction} onUpdated={setSplashPhotoUrl} />
          )}
          {activeView === 'album' && (
            <div className="topbar-stat">
              <Heart size={18} />
              <span>{favoriteMedia.length}</span>
            </div>
          )}
        </header>

        {error && <div className="alert error">{error}</div>}
        {status && <div className="alert success">{status}</div>}

        {activeView === 'home' && (
          <HomeView
            profile={profile}
            media={media}
            currentUser={currentUser}
            onAction={runAction}
          />
        )}
        {activeView === 'calendar' && (
          <CalendarView
            events={events}
            currentUser={currentUser}
            onRefresh={refreshAll}
            onAction={runAction}
          />
        )}
        {activeView === 'album' && (
          <AlbumView
            events={events}
            media={media}
            currentUser={currentUser}
            openMediaId={pendingMediaId}
            onMediaDeepLinkOpened={() => setPendingMediaId(null)}
            onAction={runAction}
          />
        )}
        {activeView === 'hiking' && (
          <HikingView records={hikingRecords} currentUser={currentUser} onAction={runAction} />
        )}
      </main>
    </div>
    <AppSplash active={showAppSplash} photoUrl={splashPhotoUrl} />
    </>
  )
}

function AdminDashboard({ currentUser, onLogout }) {
  const currentYear = todayInKorea().getFullYear()
  const defaultFilters = {
    startDate: `${currentYear}-01-01`,
    endDate: `${currentYear}-12-31`,
  }
  const [metrics, setMetrics] = useState(null)
  const [filters, setFilters] = useState(defaultFilters)
  const [appliedFilters, setAppliedFilters] = useState(defaultFilters)
  const [activePreset, setActivePreset] = useState('thisYear')
  const [error, setError] = useState('')

  const applyFilters = (nextFilters = filters, preset = 'custom') => {
    const normalizedFilters = {
      startDate: nextFilters.startDate || '',
      endDate: nextFilters.endDate || '',
    }
    setFilters(normalizedFilters)
    setAppliedFilters(normalizedFilters)
    setActivePreset(preset)
    setMetrics(null)
  }

  useEffect(() => {
    let cancelled = false

    async function loadMetrics() {
      setError('')
      try {
        const params = new URLSearchParams()
        if (appliedFilters.startDate) params.set('startDate', appliedFilters.startDate)
        if (appliedFilters.endDate) params.set('endDate', appliedFilters.endDate)
        const data = await request(`/api/admin/metrics?${params.toString()}`)
        if (!cancelled) setMetrics(data)
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }

    loadMetrics()
    return () => {
      cancelled = true
    }
  }, [appliedFilters])

  const summary = metrics?.summary || {}
  const applyPreset = (startDate, endDate, preset) => {
    applyFilters({ startDate, endDate }, preset)
  }
  const thisYear = todayInKorea().getFullYear()

  return (
    <main className="admin-screen">
      <header className="admin-header">
        <div>
          <strong className="brand-name">DANDUL</strong>
          <span>DATA MONITOR</span>
        </div>
        <div className="brand-actions">
          <span className="user-badge user-badge-admin">{currentUser.nickname}</span>
          <button type="button" className="brand-logout" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      </header>

      {error && <div className="alert error">{error}</div>}
      {!metrics ? (
        <div className="admin-loading">데이터를 모으는 중이에요.</div>
      ) : (
        <section className="admin-dashboard">
          <div className="admin-hero">
            <div>
              <span>DANDUL INSIGHT</span>
              <h1>우리 기록이 쌓이는 흐름</h1>
            </div>
            <p>게시글, 댓글, 데이트 일정, 방문 기록을 한국 시간 기준으로 집계합니다.</p>
          </div>

          <form
            className="admin-filter-panel"
            onSubmit={(event) => {
              event.preventDefault()
              applyFilters()
            }}
          >
            <label>
              시작일
              <input
                type="date"
                value={filters.startDate}
                onChange={(event) => {
                  setActivePreset('custom')
                  setFilters({ ...filters, startDate: event.target.value })
                }}
              />
            </label>
            <label>
              종료일
              <input
                type="date"
                value={filters.endDate}
                onChange={(event) => {
                  setActivePreset('custom')
                  setFilters({ ...filters, endDate: event.target.value })
                }}
              />
            </label>
            <div className="admin-filter-actions">
              <button type="button" className="primary-button" onClick={() => applyFilters()}>적용</button>
              <button
                type="button"
                className={`ghost-button preset-button ${activePreset === 'thisYear' ? 'active' : ''}`}
                onClick={() => applyPreset(`${thisYear}-01-01`, `${thisYear}-12-31`, 'thisYear')}
              >
                올해
              </button>
              <button
                type="button"
                className={`ghost-button preset-button ${activePreset === 'lastYear' ? 'active' : ''}`}
                onClick={() => applyPreset(`${thisYear - 1}-01-01`, `${thisYear - 1}-12-31`, 'lastYear')}
              >
                작년
              </button>
              <button
                type="button"
                className={`ghost-button preset-button ${activePreset === 'all' ? 'active' : ''}`}
                onClick={() => applyPreset('', '', 'all')}
              >
                전체
              </button>
            </div>
          </form>

          <div className="metric-grid">
            <MetricCard label="게시글" value={summary.totalPosts} suffix="개" />
            <MetricCard label="댓글" value={summary.totalComments} suffix="개" />
            <MetricCard label="같이 일정" value={summary.togetherEvents} suffix="개" />
            <MetricCard label="앱 방문" value={summary.totalVisits} suffix="회" />
            <MetricCard label="즐겨찾기" value={summary.favoritePosts} suffix="개" />
            <MetricCard label="등산 고도" value={summary.totalElevationMeter} suffix="m" />
          </div>

          <div className="admin-chart-grid">
            <AdminChartCard title="월별 게시글">
              <BarChart data={metrics.monthlyPosts} color="#ef9ca8" />
            </AdminChartCard>

            <AdminChartCard title="월별 댓글">
              <BarChart data={metrics.monthlyComments} color="#8cb6e8" />
            </AdminChartCard>

            <AdminChartCard title="같이 일정 월별">
              <BarChart data={metrics.monthlyTogetherEvents} color="#9acfb0" />
            </AdminChartCard>

            <AdminChartCard title="앱 방문 추이">
              <GroupedBarChart
                data={(metrics.monthlyVisits || []).map((item) => ({
                  label: item.label,
                  junhong: item.junhong,
                  somin: item.somin,
                }))}
                series={[
                  { key: 'junhong', label: '준홍', color: '#9fc5ee' },
                  { key: 'somin', label: '소민', color: '#f2a9b3' },
                ]}
              />
            </AdminChartCard>

            <AdminChartCard title="방문자별 누적">
              <UserBars data={metrics.visitsByUser} />
            </AdminChartCard>

            <AdminChartCard title="기록 작성자 비중">
              <UserBars data={metrics.contentByUser} />
            </AdminChartCard>

            <AdminChartCard title="사진 / 동영상">
              <BarChart data={metrics.mediaTypeCounts} color="#c7a7e8" />
            </AdminChartCard>

            <AdminChartCard title="월별 등산 기록">
              <BarChart data={metrics.monthlyHikingRecords} color="#8fc7c1" />
            </AdminChartCard>
          </div>
        </section>
      )}
    </main>
  )
}

function MetricCard({ label, value = 0, suffix }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>
        {Number(value || 0).toLocaleString('ko-KR')}
        <small>{suffix}</small>
      </strong>
    </article>
  )
}

function AdminChartCard({ title, children }) {
  return (
    <article className="admin-card">
      <div className="section-title">
        <h2>{title}</h2>
      </div>
      {children}
    </article>
  )
}

function maxSeriesValue(data = [], series = [{ key: 'count' }]) {
  return Math.max(1, ...data.flatMap((item) => series.map((entry) => Number(item[entry.key] ?? item.count ?? 0))))
}

function BarChart({ data = [], color }) {
  const [tooltip, setTooltip] = useState('')
  const maxValue = Math.max(1, ...data.map((item) => item.count || 0))
  return (
    <div
      className="bar-chart"
      style={{ '--bar-count': Math.max(1, data.length) }}
      onMouseLeave={() => setTooltip('')}
    >
      {tooltip && <span className="chart-floating-tooltip">{tooltip}</span>}
      {data.map((item) => {
        const tooltipText = Number(item.count || 0).toLocaleString('ko-KR')
        return (
          <div key={item.label} className="bar-item">
            <div className="bar-track">
              <button
                type="button"
                className="bar-segment"
                onClick={() => setTooltip(tooltip === tooltipText ? '' : tooltipText)}
                onFocus={() => setTooltip(tooltipText)}
                onMouseEnter={() => setTooltip(tooltipText)}
                style={{ height: `${Math.max(4, ((item.count || 0) / maxValue) * 100)}%`, background: color }}
              />
            </div>
            <strong>{item.count || 0}</strong>
            <small>{compactBucketLabel(item.label)}</small>
          </div>
        )
      })}
    </div>
  )
}

function GroupedBarChart({ data = [], series = [] }) {
  const [tooltip, setTooltip] = useState('')
  const maxValue = maxSeriesValue(data, series)
  return (
    <div
      className="bar-chart grouped"
      style={{ '--bar-count': Math.max(1, data.length) }}
      onMouseLeave={() => setTooltip('')}
    >
      {tooltip && <span className="chart-floating-tooltip">{tooltip}</span>}
      {data.map((item) => (
        <div key={item.label} className="bar-item">
          <div className="bar-track grouped-track">
            {series.map((entry) => {
              const tooltipText = `${entry.label} ${Number(item[entry.key] || 0).toLocaleString('ko-KR')}`
              return (
                <button
                  type="button"
                  className="bar-segment"
                  key={entry.key}
                  onClick={() => setTooltip(tooltip === tooltipText ? '' : tooltipText)}
                  onFocus={() => setTooltip(tooltipText)}
                  onMouseEnter={() => setTooltip(tooltipText)}
                  style={{
                    height: `${Math.max(4, ((item[entry.key] || 0) / maxValue) * 100)}%`,
                    background: entry.color,
                  }}
                />
              )
            })}
          </div>
          <small>{compactBucketLabel(item.label)}</small>
        </div>
      ))}
    </div>
  )
}

function UserBars({ data = [] }) {
  const maxValue = Math.max(1, ...data.map((item) => item.count || 0))
  return (
    <div className="user-bars">
      {data.map((item) => (
        <div key={item.username} className="user-bar-row">
          <span>{item.nickname}</span>
          <div>
            <i style={{ width: `${((item.count || 0) / maxValue) * 100}%` }} />
          </div>
          <strong>{Number(item.count || 0).toLocaleString('ko-KR')}</strong>
        </div>
      ))}
    </div>
  )
}

function compactBucketLabel(label = '') {
  if (label.includes('-')) {
    const [year, month] = label.split('-')
    const currentYear = String(todayInKorea().getFullYear())
    const monthLabel = `${Number(month)}월`
    return year === currentYear ? monthLabel : `${year.slice(2)}.${monthLabel}`
  }
  return label
}

function AppSplash({ active, photoUrl = DEFAULT_SPLASH_PHOTO_URL }) {
  if (!active) return null

  return (
    <div className="app-splash" aria-label="DANDUL loading">
      <div className="app-splash-photo">
        <img
          src={photoUrl}
          alt=""
          onError={(event) => {
            if (!event.currentTarget.src.endsWith(DEFAULT_SPLASH_PHOTO_URL)) {
              event.currentTarget.src = DEFAULT_SPLASH_PHOTO_URL
            }
          }}
        />
      </div>
      <div className="app-splash-content">
        <strong className="brand-name">DANDUL</strong>
        <span>준홍 ❤ 소민</span>
      </div>
    </div>
  )
}

function loadCropImage(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('선택한 이미지를 불러오지 못했어요.'))
    image.src = source
  })
}

async function createCroppedSplashFile(source, cropPixels) {
  const image = await loadCropImage(source)
  const canvas = document.createElement('canvas')
  canvas.width = 1080
  canvas.height = 2340
  const context = canvas.getContext('2d')
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.fillStyle = '#f8e1e5'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(
    image,
    cropPixels.x,
    cropPixels.y,
    cropPixels.width,
    cropPixels.height,
    0,
    0,
    canvas.width,
    canvas.height,
  )
  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (result) => (result ? resolve(result) : reject(new Error('이미지를 자르지 못했어요.'))),
      'image/jpeg',
      0.9,
    )
  })
  return new File([blob], 'dandul-splash.jpg', { type: 'image/jpeg' })
}

function SplashPhotoControl({ onAction, onUpdated }) {
  const inputRef = useRef(null)
  const [source, setSource] = useState(null)
  const [crop, setCrop] = useState({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [cropPixels, setCropPixels] = useState(null)
  const [saving, setSaving] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => {
    if (!source) return undefined
    return () => URL.revokeObjectURL(source)
  }, [source])

  const closeEditor = () => {
    if (saving) return
    setSource(null)
    setCrop({ x: 0, y: 0 })
    setZoom(1)
    setCropPixels(null)
    setLocalError('')
  }

  const selectFile = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setLocalError('이미지 파일을 선택해 주세요.')
      return
    }
    if (file.size > 30 * 1024 * 1024) {
      setLocalError('30MB 이하의 이미지를 선택해 주세요.')
      return
    }
    setLocalError('')
    setCrop({ x: 0, y: 0 })
    setZoom(1)
    setCropPixels(null)
    setSource(URL.createObjectURL(file))
  }

  const saveSplashPhoto = async (event) => {
    event.preventDefault()
    if (!source || !cropPixels || saving) return
    setSaving(true)
    let updatedProfile = null
    const saved = await onAction('스플래시 이미지를 변경했어요.', async () => {
      const croppedFile = await createCroppedSplashFile(source, cropPixels)
      const formData = new FormData()
      formData.append('file', croppedFile)
      updatedProfile = await request('/api/profile/splash', {
        method: 'POST',
        body: formData,
      })
      return updatedProfile
    })
    setSaving(false)
    if (!saved || !updatedProfile?.splashPhotoUrl) return
    localStorage.setItem(SPLASH_STORAGE_KEY, updatedProfile.splashPhotoUrl)
    onUpdated(updatedProfile.splashPhotoUrl)
    closeEditor()
  }

  return (
    <>
      <div className="splash-photo-control">
        <input
          ref={inputRef}
          className="splash-file-input"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={selectFile}
        />
        <button type="button" className="ghost-button" onClick={() => inputRef.current?.click()}>
          <ImageIcon size={16} />
          <span>스플래시 변경</span>
        </button>
        {localError && <span className="splash-control-error">{localError}</span>}
      </div>

      {source && (
        <div className="form-modal-backdrop" onClick={closeEditor}>
          <form
            className="form-modal splash-editor-modal"
            onSubmit={saveSplashPhoto}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="section-title">
              <h2>스플래시 편집</h2>
              <button type="button" className="modal-close-button" onClick={closeEditor}>
                <X size={20} />
              </button>
            </div>
            <div className="splash-crop-stage">
              <Cropper
                image={source}
                crop={crop}
                zoom={zoom}
                aspect={SPLASH_ASPECT}
                objectFit="cover"
                showGrid
                onCropChange={setCrop}
                onZoomChange={setZoom}
                onCropComplete={(_, pixels) => setCropPixels(pixels)}
              />
            </div>
            <label className="splash-zoom-control">
              확대
              <input
                type="range"
                min="1"
                max="3"
                step="0.01"
                value={zoom}
                onChange={(event) => setZoom(Number(event.target.value))}
              />
            </label>
            <div className="splash-editor-actions">
              <button type="button" className="ghost-button" onClick={closeEditor} disabled={saving}>
                취소
              </button>
              <button type="submit" className="primary-button" disabled={saving || !cropPixels}>
                <Upload size={17} />
                <span>{saving ? '저장 중' : '적용'}</span>
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  )
}

function OnboardingView({ onStart }) {
  return (
    <main className="onboarding-screen">
      <Waves
        className="onboarding-waves"
        lineColor="rgba(199, 91, 101, 0.22)"
        backgroundColor="rgba(255, 245, 246, 0.72)"
        waveSpeedX={0.018}
        waveSpeedY={0.008}
        waveAmpX={36}
        waveAmpY={18}
        friction={0.91}
        tension={0.008}
        maxCursorMove={90}
        xGap={14}
        yGap={34}
      />
      <section className="onboarding-panel" aria-label="DANDUL 시작 화면">
        <div className="onboarding-copy">
          <div className="onboarding-brand">
            <strong className="brand-name">DANDUL</strong>
            <span className="brand-couple">준홍 ❤ 소민</span>
          </div>
          <p>2024.03.30 (토) ~ ❤</p>
          <button type="button" className="primary-button onboarding-start" onClick={onStart}>
            START
          </button>
        </div>
      </section>
    </main>
  )
}

function LoginView({ onLogin }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')

  const submitLogin = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await onLogin(form)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <main className="login-screen">
      <form className="login-panel" onSubmit={submitLogin}>
        <div className="brand login-brand">
          <div className="brand-copy">
            <strong className="brand-name">DANDUL</strong>
            <span className="brand-couple">준홍 ❤ 소민</span>
          </div>
        </div>
        {error && <div className="alert error">{error}</div>}
        <label>
          아이디
          <input
            required
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            placeholder="junhong 또는 somin"
          />
        </label>
        <label>
          비밀번호
          <input
            required
            type="password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            placeholder="비밀번호"
          />
        </label>
        <button type="submit" className="primary-button onboarding-start">
          LOGIN
        </button>
      </form>
    </main>
  )
}

function HomeView({ profile, media, currentUser, onAction }) {
  const recentFavorites = [...media]
    .filter((item) => item.favorite)
    .sort(compareMediaByDateDesc)
    .slice(0, 10)
  const daysTogether = profile?.daysTogether ?? 0
  const anniversaries = upcomingAnniversaries(daysTogether).slice(0, 10)

  return (
    <section className="home-grid">
      <div className="summary-panel">
        <h2>우리의 시간</h2>
        <strong className="dday">{daysTogether.toLocaleString('ko-KR')}일</strong>
        <p className="muted">{formatDateWithWeekday(START_DATE)} ~ ❤</p>

        <div className="anniversary-list">
          {anniversaries.map((item) => (
            <article key={item.id} className="anniversary-card">
              <div>
                <strong>{item.title}</strong>
                <span>{item.type}</span>
                <span>{formatDateWithWeekday(toDateInput(item.date))}</span>
              </div>
              <em>{ddayLabel(item.daysUntil)}</em>
            </article>
          ))}
        </div>
      </div>

      <div className="wide-panel">
        <div className="section-title">
          <h2>즐겨찾는 추억</h2>
          <span>{recentFavorites.length}개</span>
        </div>
        <MediaGrid
          items={recentFavorites}
          compact
          horizontal
          showCardActions={false}
          currentUser={currentUser}
          onAction={onAction}
        />
      </div>
    </section>
  )
}

const GOOGLE_CALENDAR_RECONNECT_MESSAGE =
  "Google Calendar 연결이 만료되었어요. Google Cloud의 게시 상태를 확인한 뒤 '권한 갱신'을 눌러 다시 연결해 주세요."

function googleCalendarNeedsReconnect(message = '') {
  const normalized = message.toLowerCase()
  return (
    normalized.includes('expired or revoked') ||
    normalized.includes('invalid_grant') ||
    normalized.includes('token has been expired')
  )
}

function CalendarView({ events, currentUser, onRefresh, onAction }) {
  const [currentMonth, setCurrentMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })
  const [form, setForm] = useState(() => createEmptyEventForm())
  const [selectedDate, setSelectedDate] = useState(form.date)
  const [focusedEventId, setFocusedEventId] = useState(null)
  const [isDateModalOpen, setIsDateModalOpen] = useState(false)
  const [calendarModalMode, setCalendarModalMode] = useState('details')
  const [isMonthPickerOpen, setIsMonthPickerOpen] = useState(false)
  const [initialGoogleCalendarFeedback] = useState(() => {
    const result = new URLSearchParams(window.location.search).get('googleCalendar')
    if (result === 'connected') {
      return {
        result,
        message: 'Google Calendar 권한 연결을 완료했어요. 동기화하면 DANDUL 캘린더가 만들어져요.',
        error: '',
      }
    }
    if (result === 'denied') {
      return { result, message: '', error: 'Google Calendar 연결이 취소되었어요.' }
    }
    if (result?.startsWith('error:')) {
      const detail = result.slice('error:'.length)
      try {
        return { result, message: '', error: decodeURIComponent(detail) }
      } catch {
        return { result, message: '', error: detail }
      }
    }
    return { result, message: '', error: '' }
  })
  const [googleCalendarStatus, setGoogleCalendarStatus] = useState(null)
  const [googleCalendarBusy, setGoogleCalendarBusy] = useState(false)
  const [googleCalendarMessage, setGoogleCalendarMessage] = useState(
    initialGoogleCalendarFeedback.message,
  )
  const [googleCalendarError, setGoogleCalendarError] = useState(
    initialGoogleCalendarFeedback.error,
  )
  const markGoogleCalendarReconnectRequired = useCallback(() => {
    setGoogleCalendarStatus((status) => ({
      ...(status || {}),
      connected: true,
      requiresReconnect: true,
    }))
    setGoogleCalendarMessage('')
    setGoogleCalendarError(GOOGLE_CALENDAR_RECONNECT_MESSAGE)
  }, [])
  const googleCalendarImportingRef = useRef(false)
  const yearWheelRef = useRef(null)
  const monthWheelRef = useRef(null)
  const todayKey = useMemo(() => toDateInput(todayInKorea()), [])
  const calendarYears = useMemo(() => {
    const baseYear = currentMonth.getFullYear()
    return Array.from({ length: 121 }, (_, index) => baseYear - 60 + index)
  }, [currentMonth])

  useEffect(() => {
    let cancelled = false

    async function loadGoogleCalendarStatus() {
      try {
        const status = await request(
          `/api/google-calendar/status?username=${encodeURIComponent(currentUser.username)}`,
        )
        if (!cancelled) setGoogleCalendarStatus(status)
      } catch (error) {
        if (!cancelled) setGoogleCalendarError(error.message)
      }
    }

    if (initialGoogleCalendarFeedback.result) {
      const params = new URLSearchParams(window.location.search)
      params.delete('googleCalendar')
      const query = params.toString()
      window.history.replaceState({}, '', `${window.location.pathname}${query ? `?${query}` : ''}`)
    }

    loadGoogleCalendarStatus()
    return () => {
      cancelled = true
    }
  }, [currentUser.username, initialGoogleCalendarFeedback.result])

  const importGoogleCalendar = useCallback(
    async ({ silent = true } = {}) => {
      if (googleCalendarImportingRef.current) return null
      googleCalendarImportingRef.current = true
      try {
        const result = await request(
          `/api/google-calendar/import?username=${encodeURIComponent(currentUser.username)}`,
          { method: 'POST' },
        )
        if (result.imported > 0 || result.updated > 0 || result.deleted > 0) {
          await onRefresh?.()
          if (!silent) {
            setGoogleCalendarMessage(
              `Google DANDUL 캘린더에서 ${result.imported}개 추가, ${result.updated}개 업데이트, ${result.deleted}개 삭제했어요.`,
            )
          }
        }
        if (result.failed > 0) {
          const firstError = result.errors?.[0] || ''
          if (googleCalendarNeedsReconnect(firstError)) {
            markGoogleCalendarReconnectRequired()
          } else {
            setGoogleCalendarError(
              `${result.failed}개 일정을 가져오지 못했어요. ${firstError}`.trim(),
            )
          }
        }
        return result
      } catch (error) {
        if (googleCalendarNeedsReconnect(error.message)) {
          markGoogleCalendarReconnectRequired()
          return null
        }
        if (!silent) throw error
        return null
      } finally {
        googleCalendarImportingRef.current = false
      }
    },
    [currentUser.username, markGoogleCalendarReconnectRequired, onRefresh],
  )

  useEffect(() => {
    if (!googleCalendarStatus?.connected || googleCalendarStatus?.requiresReconnect) return
    importGoogleCalendar()
    const intervalId = window.setInterval(() => importGoogleCalendar(), 60_000)
    return () => window.clearInterval(intervalId)
  }, [googleCalendarStatus?.connected, googleCalendarStatus?.requiresReconnect, importGoogleCalendar])

  const connectGoogleCalendar = async () => {
    setGoogleCalendarBusy(true)
    setGoogleCalendarError('')
    setGoogleCalendarMessage('')
    try {
      const response = await request(
        `/api/google-calendar/auth-url?username=${encodeURIComponent(currentUser.username)}`,
      )
      window.location.assign(response.url)
    } catch (error) {
      setGoogleCalendarError(error.message)
      setGoogleCalendarBusy(false)
    }
  }

  const syncGoogleCalendar = async () => {
    setGoogleCalendarBusy(true)
    setGoogleCalendarError('')
    setGoogleCalendarMessage('')
    try {
      const importedResult = await importGoogleCalendar({ silent: false })
      const result = await request(
        `/api/google-calendar/sync?username=${encodeURIComponent(currentUser.username)}`,
        { method: 'POST' },
      )
      const importedSummary = importedResult
        ? `Google에서 ${importedResult.imported}개 가져오기, ${importedResult.deleted}개 삭제, `
        : ''
      const summary = `${importedSummary}${result.created}개 추가, ${result.updated}개 업데이트`
      if (result.failed > 0) {
        const firstError = result.errors?.[0] || ''
        if (googleCalendarNeedsReconnect(firstError)) {
          markGoogleCalendarReconnectRequired()
        } else {
          setGoogleCalendarError(
            `${summary}, ${result.failed}개 실패했어요. ${firstError}`.trim(),
          )
        }
      } else {
        setGoogleCalendarStatus((status) => ({
          ...status,
          requiresReconnect: false,
          calendarName: 'DANDUL',
        }))
        setGoogleCalendarMessage(`DANDUL 캘린더 동기화 완료: ${summary}`)
      }
    } catch (error) {
      if (googleCalendarNeedsReconnect(error.message)) {
        markGoogleCalendarReconnectRequired()
      } else {
        setGoogleCalendarError(error.message)
      }
    } finally {
      setGoogleCalendarBusy(false)
    }
  }

  const syncEventToGoogleCalendar = useCallback(async (eventId) => {
    try {
      const result = await request(
        `/api/google-calendar/sync-event?eventId=${encodeURIComponent(eventId)}`,
        { method: 'POST' },
      )
      if (result.failed > 0) {
        const firstError = result.errors?.[0] || ''
        if (googleCalendarNeedsReconnect(firstError)) {
          markGoogleCalendarReconnectRequired()
        } else {
          setGoogleCalendarError(
            `DANDUL 일정은 저장됐지만 Google 자동 반영에 실패했어요. ${firstError}`.trim(),
          )
        }
      } else if (result.connected > 0 && result.skipped < result.connected) {
        setGoogleCalendarMessage('Google DANDUL 캘린더에 자동 반영했어요.')
      }
    } catch (error) {
      if (googleCalendarNeedsReconnect(error.message)) {
        markGoogleCalendarReconnectRequired()
      } else {
        setGoogleCalendarError(
          `DANDUL 일정은 저장됐지만 Google 자동 반영에 실패했어요. ${error.message}`,
        )
      }
    }
  }, [markGoogleCalendarReconnectRequired])

  const disconnectGoogleCalendar = async () => {
    if (!window.confirm('Google Calendar 연결을 해제할까요? 이미 내보낸 일정은 캘린더에 남아 있어요.')) {
      return
    }
    setGoogleCalendarBusy(true)
    setGoogleCalendarError('')
    setGoogleCalendarMessage('')
    try {
      await request(
        `/api/google-calendar/connection?username=${encodeURIComponent(currentUser.username)}`,
        { method: 'DELETE' },
      )
      setGoogleCalendarStatus((status) => ({ ...status, connected: false, connectedAt: null }))
      setGoogleCalendarMessage('Google Calendar 연결을 해제했어요.')
    } catch (error) {
      setGoogleCalendarError(error.message)
    } finally {
      setGoogleCalendarBusy(false)
    }
  }

  useEffect(() => {
    if (!isMonthPickerOpen) return
    window.requestAnimationFrame(() => {
      yearWheelRef.current?.querySelector('.active')?.scrollIntoView({ block: 'center' })
      monthWheelRef.current?.querySelector('.active')?.scrollIntoView({ block: 'center' })
    })
  }, [currentMonth, isMonthPickerOpen])

  const monthDays = useMemo(() => {
    const first = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1)
    const last = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0)
    const cells = []
    for (let i = 0; i < first.getDay(); i += 1) cells.push(null)
    for (let day = 1; day <= last.getDate(); day += 1) {
      cells.push(new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day))
    }
    while (cells.length % 7 !== 0) cells.push(null)
    return cells
  }, [currentMonth])

  const monthCells = useMemo(
    () =>
      monthDays.map((day, index) => {
        const dateKey = day ? toDateInput(day) : ''
        return {
          day,
          dateKey,
          index,
          row: Math.floor(index / 7) + 1,
          column: (index % 7) + 1,
          holiday: dateKey ? holidayEvent(dateKey) : null,
        }
      }),
    [monthDays],
  )
  const weekCount = Math.max(1, Math.ceil(monthCells.length / 7))

  const displayEvents = useMemo(
    () => [...events, ...monthCells.map((cell) => cell.holiday).filter(Boolean)],
    [events, monthCells],
  )

  const eventsByDate = useMemo(
    () =>
      displayEvents.reduce((acc, event) => {
        dateRangeKeys(event.date, eventEndDate(event)).forEach((dateKey) => {
          acc[dateKey] = [...(acc[dateKey] || []), event]
        })
        return acc
      }, {}),
    [displayEvents],
  )

  const calendarEventBars = useMemo(() => {
    const datedCells = monthCells.filter((cell) => cell.day)
    if (datedCells.length === 0) return []

    const firstDateKey = datedCells[0].dateKey
    const lastDateKey = datedCells[datedCells.length - 1].dateKey
    const cellByDate = Object.fromEntries(datedCells.map((cell) => [cell.dateKey, cell]))
    const sortedEvents = [...displayEvents]
      .filter((event) => event.date <= lastDateKey && eventEndDate(event) >= firstDateKey)
      .sort(
        (first, second) =>
          first.date.localeCompare(second.date) ||
          (first.meetingTime || '').localeCompare(second.meetingTime || '') ||
          first.title.localeCompare(second.title),
      )
    const rowLevels = Array.from({ length: weekCount }, () => [])

    return sortedEvents.flatMap((event) => {
      const eventStart = event.date < firstDateKey ? firstDateKey : event.date
      const eventEnd = eventEndDate(event) > lastDateKey ? lastDateKey : eventEndDate(event)
      const segments = []

      for (let row = 1; row <= weekCount; row += 1) {
        const rowCells = datedCells.filter((cell) => cell.row === row)
        if (rowCells.length === 0) continue

        const rowStart = rowCells[0].dateKey
        const rowEnd = rowCells[rowCells.length - 1].dateKey
        const segmentStart = eventStart > rowStart ? eventStart : rowStart
        const segmentEnd = eventEnd < rowEnd ? eventEnd : rowEnd
        if (segmentStart > segmentEnd) continue

        const startCell = cellByDate[segmentStart]
        const endCell = cellByDate[segmentEnd]
        if (!startCell || !endCell) continue

        const levels = rowLevels[row - 1]
        const level = levels.findIndex((lastColumn) => lastColumn < startCell.column)
        const nextLevel = level === -1 ? levels.length : level
        levels[nextLevel] = endCell.column

        segments.push({
          event,
          row,
          level: nextLevel,
          startColumn: startCell.column,
          span: endCell.column - startCell.column + 1,
          dateKey: segmentStart,
          startsInSegment: segmentStart === event.date,
          endsInSegment: segmentEnd === eventEndDate(event),
        })
      }

      return segments
    })
  }, [displayEvents, monthCells, weekCount])

  const todayEvents = useMemo(
    () =>
      displayEvents
        .filter((event) => isDateInEventRange(todayKey, event))
        .sort(
          (first, second) =>
            (first.meetingTime || '').localeCompare(second.meetingTime || '') ||
            first.title.localeCompare(second.title),
        )
        .slice(0, 5),
    [displayEvents, todayKey],
  )

  const selectedDateEvents = useMemo(() => {
    const dateEvents = [...(eventsByDate[selectedDate] || [])].sort(
      (first, second) =>
        (first.meetingTime || '').localeCompare(second.meetingTime || '') ||
        first.title.localeCompare(second.title),
    )
    return focusedEventId ? dateEvents.filter((event) => event.id === focusedEventId) : dateEvents
  }, [eventsByDate, focusedEventId, selectedDate])

  const openDateModal = (dateKey) => {
    setSelectedDate(dateKey)
    setFocusedEventId(null)
    setForm(createEmptyEventForm(dateKey))
    setCalendarModalMode('details')
    setIsDateModalOpen(true)
  }

  const openEventDetails = (event, dateKey = event.date) => {
    setSelectedDate(dateKey)
    setFocusedEventId(event.id)
    setForm(createEmptyEventForm(dateKey))
    setCalendarModalMode('details')
    setIsDateModalOpen(true)
  }

  const closeDateModal = () => {
    setIsDateModalOpen(false)
    setCalendarModalMode('details')
    setFocusedEventId(null)
    setForm(createEmptyEventForm(selectedDate))
  }

  const openNewEventForm = () => {
    setFocusedEventId(null)
    setForm(createEmptyEventForm(selectedDate))
    setCalendarModalMode('form')
  }

  const submitEvent = async (event) => {
    event.preventDefault()
    const payload = {
      title: form.title,
      date: form.date,
      endDate: form.isRange ? form.endDate : form.date,
      meetingTime: form.timeUnknown ? null : form.meetingTime || null,
      place: form.placeUnknown ? UNKNOWN_PLACE_LABEL : form.place,
      memo: form.memo,
      category: eventCategory(form.category),
      createdBy: form.createdBy || currentUser.username,
      creatorNickname: form.creatorNickname || currentUser.nickname,
    }
    const path = form.id ? `/api/events/${form.id}` : '/api/events'
    const method = form.id ? 'PUT' : 'POST'
    let savedEvent = null
    const saved = await onAction(form.id ? '데이트 일정을 수정했어요.' : '데이트 일정을 추가했어요.', async () => {
      savedEvent = await request(path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      return savedEvent
    })
    if (saved) {
      setSelectedDate(form.date)
      setForm(createEmptyEventForm(form.date))
      setIsDateModalOpen(false)
      if (savedEvent?.id) syncEventToGoogleCalendar(savedEvent.id)
    }
  }

  const editEvent = (event) => {
    setSelectedDate(event.date)
    setFocusedEventId(event.id)
    setIsDateModalOpen(true)
    setCalendarModalMode('form')
    setForm({
      id: event.id,
      title: event.title,
      date: event.date,
      endDate: eventEndDate(event),
      isRange: eventEndDate(event) !== event.date,
      meetingTime: event.meetingTime?.slice(0, 5) || '',
      timeUnknown: !event.meetingTime,
      place: event.place === UNKNOWN_PLACE_LABEL ? '' : event.place,
      placeUnknown: event.place === UNKNOWN_PLACE_LABEL,
      memo: event.memo || '',
      category: eventCategory(event.category),
      createdBy: event.createdBy || DEFAULT_AUTHOR.username,
      creatorNickname: authorNickname(event),
    })
  }

  const deleteEvent = async (id) => {
    if (form.id === id) {
      setForm(createEmptyEventForm(selectedDate))
      setCalendarModalMode('details')
    }
    if (focusedEventId === id) {
      setFocusedEventId(null)
    }
    const deleted = await onAction('데이트 일정을 삭제했어요.', () =>
      request(`/api/events/${id}`, {
        method: 'DELETE',
      }),
    )
    if (deleted) syncEventToGoogleCalendar(id)
  }

  const moveMonth = (amount) => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + amount, 1),
    )
  }

  const selectCalendarYear = (year) => {
    setCurrentMonth(new Date(year, currentMonth.getMonth(), 1))
  }

  const selectCalendarMonth = (monthIndex) => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), monthIndex, 1))
  }

  const moveToToday = () => {
    const today = todayInKorea()
    setCurrentMonth(new Date(today.getFullYear(), today.getMonth(), 1))
    setSelectedDate(toDateInput(today))
    setFocusedEventId(null)
    setIsMonthPickerOpen(false)
  }

  return (
    <section className="calendar-layout">
      <div className="calendar-panel">
        <div className="calendar-google-sync">
          <div className="calendar-google-copy">
            <span className="calendar-google-icon" aria-hidden="true">
              <CalendarDays size={19} />
            </span>
            <div>
              <strong>Google Calendar</strong>
              <span>
                {googleCalendarStatus?.requiresReconnect
                  ? 'DANDUL 캘린더 생성을 위한 권한 갱신 필요'
                  : googleCalendarStatus?.calendarName
                    ? `${googleCalendarStatus.calendarName} 캘린더에 연결됨`
                    : googleCalendarStatus?.connected
                      ? `${currentUser.nickname} 계정에 연결됨`
                  : googleCalendarStatus?.configured === false
                    ? '서버 설정을 확인해 주세요.'
                    : 'DANDUL 전용 캘린더로 일정 내보내기'}
              </span>
            </div>
          </div>
          <div className="calendar-google-actions">
            {googleCalendarStatus?.connected && !googleCalendarStatus?.requiresReconnect ? (
              <>
                <button
                  type="button"
                  className="calendar-google-button primary"
                  onClick={syncGoogleCalendar}
                  disabled={googleCalendarBusy}
                >
                  <RotateCw size={15} />
                  <span>{googleCalendarBusy ? '동기화 중' : '일정 동기화'}</span>
                </button>
                <button
                  type="button"
                  className="calendar-google-button"
                  onClick={disconnectGoogleCalendar}
                  disabled={googleCalendarBusy}
                >
                  연결 해제
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  className="calendar-google-button primary"
                  onClick={connectGoogleCalendar}
                  disabled={googleCalendarBusy || googleCalendarStatus?.configured === false}
                >
                  <CalendarDays size={15} />
                  <span>
                    {googleCalendarBusy
                      ? '연결 중'
                      : googleCalendarStatus?.requiresReconnect
                        ? '권한 갱신'
                        : 'Google 계정 연결'}
                  </span>
                </button>
                {googleCalendarStatus?.connected && (
                  <button
                    type="button"
                    className="calendar-google-button"
                    onClick={disconnectGoogleCalendar}
                    disabled={googleCalendarBusy}
                  >
                    연결 해제
                  </button>
                )}
              </>
            )}
          </div>
        </div>
        {googleCalendarMessage && (
          <div className="calendar-google-notice success" role="status">
            {googleCalendarMessage}
          </div>
        )}
        {googleCalendarError && (
          <div className="calendar-google-notice error" role="alert">
            {googleCalendarError}
          </div>
        )}

        <div className="calendar-overview">
          <div className="section-title">
            <h2 className="today-title">
              TODAY <span>{formatDatePlainWeekday(todayKey)}</span>
            </h2>
            <span>{todayEvents.length}개</span>
          </div>
          {todayEvents.length > 0 ? (
            <div className="calendar-upcoming-list">
              {todayEvents.map((event) => (
                <button
                  key={event.id}
                  type="button"
                  className={`upcoming-card upcoming-card-${calendarEventCategory(event)}`}
                  onClick={() => openEventDetails(event, todayKey)}
                >
                  <strong>{event.title}</strong>
                  <span>
                    <CalendarDays size={14} />
                    {formatEventDateRange(event)}
                  </span>
                  {!event.isHoliday && (
                    <span>
                      <Clock size={14} />
                      {timeLabel(event.meetingTime)}
                    </span>
                  )}
                  <span>
                    <MapPin size={14} />
                    {event.place}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <div className="empty-state compact">보고싶어! 너무 보고싶어!! 사랑해❤</div>
          )}
        </div>

        <div className="calendar-header">
          <button type="button" className="icon-only" onClick={() => moveMonth(-1)}>
            ‹
          </button>
          <div className="calendar-title-picker">
            <h2 className="calendar-title">
              <button type="button" onClick={() => setIsMonthPickerOpen((open) => !open)}>
                {currentMonth.getFullYear()}년
              </button>
              <button type="button" onClick={() => setIsMonthPickerOpen((open) => !open)}>
                {currentMonth.getMonth() + 1}월
              </button>
            </h2>
            {isMonthPickerOpen && (
              <div className="calendar-wheel-picker" role="dialog" aria-label="Calendar month picker">
                <div className="wheel-column">
                  <span>YEAR</span>
                  <div className="wheel-list" ref={yearWheelRef}>
                    {calendarYears.map((year) => (
                      <button
                        key={year}
                        type="button"
                        className={year === currentMonth.getFullYear() ? 'active' : ''}
                        onClick={() => selectCalendarYear(year)}
                      >
                        {year}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="wheel-column">
                  <span>MONTH</span>
                  <div className="wheel-list" ref={monthWheelRef}>
                    {Array.from({ length: 12 }, (_, monthIndex) => (
                      <button
                        key={monthIndex}
                        type="button"
                        className={monthIndex === currentMonth.getMonth() ? 'active' : ''}
                        onClick={() => selectCalendarMonth(monthIndex)}
                      >
                        {monthIndex + 1}월
                      </button>
                    ))}
                  </div>
                </div>
                <button
                  type="button"
                  className="ghost-button wheel-close"
                  onClick={() => setIsMonthPickerOpen(false)}
                >
                  OK
                </button>
              </div>
            )}
          </div>
          <button type="button" className="icon-only" onClick={() => moveMonth(1)}>
            ›
          </button>
          <button type="button" className="today-jump-button" onClick={moveToToday}>
            TODAY
          </button>
        </div>
        <div className="weekday-row">
          {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
            <span key={day}>{day}</span>
          ))}
        </div>
        <div className="calendar-grid" style={{ '--calendar-week-count': weekCount }}>
          {monthCells.map(({ day, dateKey, index, row, column, holiday }) => {
            const dayEvents = eventsByDate[dateKey] || []
            return (
              <button
                key={`${dateKey}-${index}`}
                type="button"
                className={[
                  'day-cell',
                  !day ? 'blank' : '',
                  dateKey === todayKey ? 'today' : '',
                  dateKey === selectedDate ? 'selected' : '',
                  holiday ? 'holiday' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                style={{ gridRow: row, gridColumn: column }}
                onClick={() => day && openDateModal(dateKey)}
              >
                {day && <span className="day-number">{day.getDate()}</span>}
                {dayEvents.length > 0 && <span className="day-event-count">{dayEvents.length}</span>}
              </button>
            )
          })}
          {calendarEventBars.map((segment) => (
            <span
              key={`${segment.event.id}-${segment.row}-${segment.startColumn}`}
              className={[
                'calendar-event-bar',
                `calendar-event-bar-${calendarEventCategory(segment.event)}`,
                segment.startsInSegment ? 'starts' : '',
                segment.endsInSegment ? 'ends' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              style={{
                gridRow: segment.row,
                gridColumn: `${segment.startColumn} / span ${segment.span}`,
                '--event-top': `${30 + segment.level * 20}px`,
              }}
              title={segment.event.title}
            >
              {segment.event.title}
            </span>
          ))}
        </div>
      </div>

      {isDateModalOpen && (
        <div className="calendar-modal-backdrop" onClick={closeDateModal}>
          <div
            className="calendar-event-modal"
            role="dialog"
            aria-modal="true"
            aria-label={`${formatDate(selectedDate)} 일정`}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="section-title">
              <div>
                <h2>{formatDate(selectedDate)}</h2>
                <span>{selectedDateEvents.length}개의 일정</span>
              </div>
              <button type="button" className="modal-close-button" onClick={closeDateModal}>
                <X size={20} />
              </button>
            </div>

            {calendarModalMode === 'details' ? (
              <div className="calendar-detail-view">
                <div className="event-detail-list">
                  {selectedDateEvents.length > 0 ? (
                    selectedDateEvents.map((event) => (
                      <article key={event.id} className="event-detail-row">
                        <div>
                          <strong>{event.title}</strong>
                          <div className="event-detail-meta">
                            <span>
                              <CalendarDays size={14} />
                              {formatEventDateRange(event)}
                            </span>
                            {!event.isHoliday && (
                              <span>
                                <Clock size={14} />
                                {timeLabel(event.meetingTime)}
                              </span>
                            )}
                            <span>
                              <MapPin size={14} />
                              {event.place}
                            </span>
                            <span
                              className={`category-badge category-${calendarEventCategory(event)}`}
                            >
                              {calendarEventCategoryLabel(event)}
                            </span>
                            {!event.isHoliday && <span className={authorBadgeClass(event)}>{authorNickname(event)}</span>}
                          </div>
                          {event.memo && <p>{event.memo}</p>}
                        </div>
                        {!event.isHoliday && <div className="row-actions">
                          <button type="button" className="icon-only" onClick={() => editEvent(event)}>
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            className="icon-only danger"
                            onClick={() => deleteEvent(event.id)}
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>}
                      </article>
                    ))
                  ) : (
                    <div className="empty-state compact">보고싶어! 너무 보고싶어!! 사랑해❤</div>
                  )}
                </div>
                <button
                  type="button"
                  className="calendar-add-button"
                  onClick={openNewEventForm}
                  aria-label="일정 추가"
                >
                  <Plus size={22} />
                </button>
              </div>
            ) : (
              <form className="stack-form calendar-event-form" onSubmit={submitEvent}>
                <div className="section-title">
                  <h2>{form.id ? '일정 수정' : '일정 추가'}</h2>
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      setForm(createEmptyEventForm(selectedDate))
                      setCalendarModalMode('details')
                    }}
                  >
                    일정 보기
                  </button>
                </div>
                <label>
                  데이트 이름
                  <input
                    required
                    value={form.title}
                    onChange={(event) => setForm({ ...form, title: event.target.value })}
                    placeholder="예: 성수 데이트"
                  />
                </label>
                <label>
                  카테고리
                  <select
                    value={form.category}
                    onChange={(event) => setForm({ ...form, category: event.target.value })}
                  >
                    {EVENT_CATEGORIES.map((category) => (
                      <option key={category.value} value={category.value}>
                        {category.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  시작일
                  <input
                    required
                    type="date"
                    value={form.date}
                    onChange={(event) => {
                      const date = event.target.value
                      setForm({
                        ...form,
                        date,
                        endDate: form.endDate < date ? date : form.endDate,
                      })
                    }}
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={form.isRange}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        isRange: event.target.checked,
                        endDate: event.target.checked ? form.endDate : form.date,
                      })
                    }
                  />
                  <span>기간 일정</span>
                </label>
                {form.isRange && (
                  <label>
                    종료일
                    <input
                      required
                      type="date"
                      min={form.date}
                      value={form.endDate}
                      onChange={(event) => setForm({ ...form, endDate: event.target.value })}
                    />
                  </label>
                )}
                <label>
                  만날 시간
                  <input
                    type="time"
                    value={form.meetingTime}
                    disabled={form.timeUnknown}
                    onChange={(event) =>
                      setForm({ ...form, meetingTime: event.target.value, timeUnknown: false })
                    }
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={form.timeUnknown}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        timeUnknown: event.target.checked,
                        meetingTime: event.target.checked ? '' : form.meetingTime,
                      })
                    }
                  />
                  <span>시간 미정</span>
                </label>
                <label>
                  장소
                  <input
                    required
                    value={form.place}
                    disabled={form.placeUnknown}
                    onChange={(event) =>
                      setForm({ ...form, place: event.target.value, placeUnknown: false })
                    }
                    placeholder="예: 서울숲역 3번 출구"
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={form.placeUnknown}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        placeUnknown: event.target.checked,
                        place: event.target.checked ? '' : form.place,
                      })
                    }
                  />
                  <span>장소 미정</span>
                </label>
                <label>
                  메모
                  <textarea
                    value={form.memo}
                    onChange={(event) => setForm({ ...form, memo: event.target.value })}
                    rows="4"
                  />
                </label>
                <button type="submit" className="primary-button">
                  <Plus size={17} />
                  <span>{form.id ? '수정' : '추가'}</span>
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </section>
  )
}

function AlbumView({ events, media, currentUser, openMediaId, onMediaDeepLinkOpened, onAction }) {
  const [filter, setFilter] = useState('ALL')
  const [form, setForm] = useState(() => createEmptyMediaForm())
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
  const [isEventPickerOpen, setIsEventPickerOpen] = useState(false)
  const eventOptionRefs = useRef({})

  const sortedMedia = useMemo(() => [...media].sort(compareMediaByDateDesc), [media])
  const sortedEventOptions = useMemo(() => {
    return [...events].sort(compareEventsByDateDesc)
  }, [events])
  const closestEventId = useMemo(() => {
    const today = todayInKorea()
    return [...events].sort((first, second) => compareEventsByClosestDate(first, second, today))[0]?.id || ''
  }, [events])
  const linkedEvent = events.find((event) => event.id === form.eventId)
  const filteredMedia = sortedMedia.filter((item) => {
    if (filter === 'ALL') return true
    if (filter === 'FAVORITE') return item.favorite
    return item.mediaType === filter
  })

  useEffect(() => {
    if (!isEventPickerOpen || !closestEventId) return
    requestAnimationFrame(() => {
      eventOptionRefs.current[closestEventId]?.scrollIntoView({ block: 'center' })
    })
  }, [closestEventId, isEventPickerOpen])

  const uploadMedia = (event) => {
    event.preventDefault()
    if (!form.file) return
    const formElement = event.currentTarget
    const snapshot = { ...form }

    onAction('앨범에 추억을 추가했어요.', async () => {
      const uploadFile =
        snapshot.mediaType === 'PHOTO' ? await normalizePhotoForUpload(snapshot.file) : snapshot.file
      const body = new FormData()
      body.append('file', uploadFile)
      body.append('mediaType', snapshot.mediaType)
      body.append('rotationDegrees', 0)
      if (snapshot.eventId) body.append('eventId', snapshot.eventId)
      if (snapshot.title) body.append('title', snapshot.title)
      if (snapshot.memo) body.append('memo', snapshot.memo)
      if (snapshot.capturedAt) body.append('capturedAt', snapshot.capturedAt)
      body.append('createdBy', currentUser.username)
      body.append('creatorNickname', currentUser.nickname)
      return request('/api/media', {
        method: 'POST',
        body,
      })
    }).then((success) => {
      if (!success) return
      setForm(createEmptyMediaForm())
      setIsUploadModalOpen(false)
      setIsEventPickerOpen(false)
      formElement.reset()
    })
  }

  return (
    <section className="album-layout">
      <div className="media-panel">
        <div className="section-title album-filter-row">
          <div className="tabs">
            {[
              ['ALL', '전체'],
              ['PHOTO', '사진'],
              ['VIDEO', '동영상'],
              ['FAVORITE', '❤'],
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                className={filter === value ? 'active' : ''}
                onClick={() => setFilter(value)}
              >
                {label}
              </button>
            ))}
          </div>
          <button
            type="button"
            className="primary-button album-upload-button"
            onClick={() => {
              setForm(createEmptyMediaForm())
              setIsEventPickerOpen(false)
              setIsUploadModalOpen(true)
            }}
          >
            <Plus size={17} />
            <span>등록</span>
          </button>
        </div>
        <MediaGrid
          items={filteredMedia}
          events={events}
          instagram
          currentUser={currentUser}
          openMediaId={openMediaId}
          onMediaDeepLinkOpened={onMediaDeepLinkOpened}
          onAction={onAction}
        />
      </div>

      {isUploadModalOpen && (
        <div className="form-modal-backdrop" onClick={() => setIsUploadModalOpen(false)}>
          <form
            className="form-modal stack-form"
            onSubmit={uploadMedia}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="section-title">
              <h2>추억 등록</h2>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setIsUploadModalOpen(false)}
              >
                <X size={20} />
              </button>
            </div>
            <div className="segmented">
              {[
                ['PHOTO', ImageIcon, '사진'],
                ['VIDEO', Film, '동영상'],
              ].map(([value, Icon, label]) => (
                <button
                  key={value}
                  type="button"
                  className={form.mediaType === value ? 'active' : ''}
                  onClick={() =>
                    setForm({
                      ...form,
                      mediaType: value,
                      rotationDegrees: value === 'PHOTO' ? form.rotationDegrees : 0,
                    })
                  }
                >
                  <Icon size={16} />
                  <span>{label}</span>
                </button>
              ))}
            </div>
            {form.mediaType === 'PHOTO' && (
              <label>
                사진 회전
                <div className="segmented rotation-options">
                  {[
                    [0, '원본'],
                    [90, '90도'],
                    [180, '180도'],
                    [270, '270도'],
                  ].map(([value, label]) => (
                    <button
                      key={value}
                      type="button"
                      className={form.rotationDegrees === value ? 'active' : ''}
                      onClick={() => setForm({ ...form, rotationDegrees: value })}
                    >
                      <RotateCw size={15} />
                      <span>{label}</span>
                    </button>
                  ))}
                </div>
              </label>
            )}
            <label>
              연결할 일정
              <div className="event-picker">
                <button
                  type="button"
                  className="event-picker-toggle"
                  onClick={() => setIsEventPickerOpen((open) => !open)}
                >
                  <span>
                    {linkedEvent
                      ? `${linkedEvent.date}${linkedEvent.endDate && linkedEvent.endDate !== linkedEvent.date ? `~${linkedEvent.endDate}` : ''} · ${linkedEvent.title}`
                      : '일정 없이 등록'}
                  </span>
                </button>
                {isEventPickerOpen && (
                  <div className="event-picker-list" role="listbox">
                    <button
                      type="button"
                      className={`event-picker-option ${!form.eventId ? 'active' : ''}`}
                      onClick={() => {
                        setForm({ ...form, eventId: '' })
                        setIsEventPickerOpen(false)
                      }}
                    >
                      일정 없이 등록
                    </button>
                    {sortedEventOptions.map((event) => (
                      <button
                        key={event.id}
                        type="button"
                        ref={(element) => {
                          if (element) eventOptionRefs.current[event.id] = element
                        }}
                        className={[
                          'event-picker-option',
                          event.id === form.eventId ? 'active' : '',
                          event.id === closestEventId ? 'closest' : '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                        onClick={() => {
                          setForm({
                            ...form,
                            eventId: event.id,
                            capturedAt: event.date || form.capturedAt,
                          })
                          setIsEventPickerOpen(false)
                        }}
                      >
                        <span>
                          {event.date}
                          {event.endDate && event.endDate !== event.date ? `~${event.endDate}` : ''}
                        </span>
                        <strong>{event.title}</strong>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </label>
            <label>
              제목
              <input
                value={form.title}
                onChange={(event) => setForm({ ...form, title: event.target.value })}
                placeholder="예: 한강 노을"
              />
            </label>
            <label>
              촬영일
              <input
                type="date"
                value={form.capturedAt}
                onChange={(event) => setForm({ ...form, capturedAt: event.target.value })}
              />
            </label>
            <label>
              메모
              <textarea
                rows="3"
                value={form.memo}
                onChange={(event) => setForm({ ...form, memo: event.target.value })}
              />
            </label>
            <label className="drop-zone">
              <Upload size={22} />
              <span>{form.file ? form.file.name : '파일 선택'}</span>
              <input
                required
                type="file"
                accept={form.mediaType === 'PHOTO' ? 'image/*' : 'video/*'}
                onChange={(event) => setForm({ ...form, file: event.target.files?.[0] || null })}
              />
            </label>
            <button type="submit" className="primary-button">
              <Plus size={17} />
              <span>등록</span>
            </button>
          </form>
        </div>
      )}

    </section>
  )
}

function HikingView({ records, currentUser, onAction }) {
  const [entryMode, setEntryMode] = useState('bac')
  const [selectedMountainId, setSelectedMountainId] = useState(bacMountains[0]?.id || '')
  const [climbedAt, setClimbedAt] = useState(toDateInput(new Date()))
  const [memo, setMemo] = useState('')
  const [keyword, setKeyword] = useState('')
  const [range, setRange] = useState({ start: emptyDateParts(), end: emptyDateParts() })
  const [isHikingFormOpen, setIsHikingFormOpen] = useState(false)
  const [isMapExpanded, setIsMapExpanded] = useState(false)
  const [isHistoryExpanded, setIsHistoryExpanded] = useState(false)
  const [editingRecordId, setEditingRecordId] = useState(null)
  const [editForm, setEditForm] = useState(null)
  const [customMountain, setCustomMountain] = useState({
    mountainName: '',
    location: '',
    elevationMeter: '',
    latitude: '',
    longitude: '',
  })

  const selectedMountain = bacMountains.find((mountain) => mountain.id === selectedMountainId) || bacMountains[0]
  const filteredMountains = bacMountains.filter((mountain) =>
    `${mountain.name} ${mountain.location}`.toLowerCase().includes(keyword.toLowerCase()),
  )
  const rangeStart = datePartsToInput(range.start)
  const rangeEnd = datePartsToInput(range.end)
  const hasRangeSelection = Object.values(range.start).some(Boolean) || Object.values(range.end).some(Boolean)
  const filterYears = useMemo(() => {
    const currentYear = todayInKorea().getFullYear()
    const recordYears = records
      .map((record) => Number(record.climbedAt?.slice(0, 4)))
      .filter(Number.isFinite)
    const firstYear = Math.min(currentYear - 10, ...recordYears)
    const lastYear = Math.max(currentYear + 1, ...recordYears)
    return Array.from({ length: lastYear - firstYear + 1 }, (_, index) => lastYear - index)
  }, [records])
  const filteredRecords = records.filter((record) => {
    const afterStart = !rangeStart || record.climbedAt >= rangeStart
    const beforeEnd = !rangeEnd || record.climbedAt <= rangeEnd
    return afterStart && beforeEnd
  })
  const previousMountains = useMemo(() => {
    const mountainMap = new Map()
    records.forEach((record) => {
      const key = normalizeMountainName(record.mountainName)
      if (!key || mountainMap.has(key)) return
      mountainMap.set(key, record)
    })
    return [...mountainMap.values()]
  }, [records])
  const totalElevation = filteredRecords.reduce((sum, record) => sum + record.elevationMeter, 0)
  const uniqueMountainCount = new Set(
    filteredRecords.map((record) => normalizeMountainName(record.mountainName)).filter(Boolean),
  ).size
  const completedBacIds = new Set(
    filteredRecords
      .filter(isBacRecord)
      .map((record) => findBacMountainForRecord(record)?.id)
      .filter(Boolean),
  )
  const customLatitude = parseCoordinate(customMountain.latitude, -90, 90)
  const customLongitude = parseCoordinate(customMountain.longitude, -180, 180)
  const customPreviewRecord =
    entryMode === 'custom' && customLatitude !== null && customLongitude !== null
      ? {
          id: 'custom-preview',
          mountainName: customMountain.mountainName || '직접 입력 위치',
          location: customMountain.location || '입력 좌표',
          elevationMeter: Number(customMountain.elevationMeter) || 0,
          latitude: customLatitude,
          longitude: customLongitude,
          isPreview: true,
        }
      : null

  const findPreviousMountain = (mountainName) =>
    previousMountains.find((record) => normalizeMountainName(record.mountainName) === normalizeMountainName(mountainName))

  const applyPreviousMountain = (mountainName, baseForm) => {
    const matched = findPreviousMountain(mountainName)
    if (!matched) return { ...baseForm, mountainName }
    const officialMountain = findBacMountainForRecord(matched)
    const shouldUseOfficialCoordinates =
      officialMountain && (!hasValidCoordinates(matched) || isLikelyLegacyEstimatedCoordinate(matched))

    return {
      ...baseForm,
      mountainName,
      location: matched.location,
      elevationMeter: String(matched.elevationMeter),
      latitude: shouldUseOfficialCoordinates ? officialMountain.latitude : matched.latitude ?? '',
      longitude: shouldUseOfficialCoordinates ? officialMountain.longitude : matched.longitude ?? '',
      source: matched.source || (isBacRecord(matched) ? 'bac' : 'custom'),
    }
  }

  const buildHikingPayload = (form) => ({
    mountainName: form.mountainName,
    location: form.location,
    elevationMeter: Number(form.elevationMeter),
    climbedAt: form.climbedAt,
    latitude: parseCoordinate(form.latitude, -90, 90),
    longitude: parseCoordinate(form.longitude, -180, 180),
    source: form.source || 'custom',
    memo: form.memo,
    createdBy: form.createdBy || currentUser.username,
    creatorNickname: form.creatorNickname || currentUser.nickname,
  })

  const submitRecord = (event) => {
    event.preventDefault()
    const payload =
      entryMode === 'bac'
        ? {
            mountainName: selectedMountain.name,
            location: selectedMountain.location,
            elevationMeter: selectedMountain.elevationMeter,
            climbedAt,
            latitude: selectedMountain.latitude,
            longitude: selectedMountain.longitude,
            source: 'bac',
            memo,
            createdBy: currentUser.username,
            creatorNickname: currentUser.nickname,
          }
        : {
            mountainName: customMountain.mountainName,
            location: customMountain.location,
            elevationMeter: Number(customMountain.elevationMeter),
            climbedAt,
            latitude: customLatitude,
            longitude: customLongitude,
            source: 'custom',
            memo,
            createdBy: currentUser.username,
            creatorNickname: currentUser.nickname,
          }
    onAction('등산 이력을 추가했어요.', () =>
      request('/api/hiking-records', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }),
    )
    setIsHikingFormOpen(false)
    setMemo('')
    if (entryMode === 'custom') {
      setCustomMountain({
        mountainName: '',
        location: '',
        elevationMeter: '',
        latitude: '',
        longitude: '',
      })
    }
  }

  const startEditRecord = (record) => {
    setEditingRecordId(record.id)
    setEditForm({
      mountainName: record.mountainName,
      location: record.location,
      elevationMeter: String(record.elevationMeter),
      climbedAt: record.climbedAt,
      latitude: record.latitude ?? '',
      longitude: record.longitude ?? '',
      source: record.source || (isBacRecord(record) ? 'bac' : 'custom'),
      memo: record.memo || '',
      createdBy: record.createdBy || DEFAULT_AUTHOR.username,
      creatorNickname: authorNickname(record),
    })
  }

  const submitEditRecord = (event) => {
    event.preventDefault()
    if (!editingRecordId || !editForm) return
    const id = editingRecordId
    const payload = buildHikingPayload(editForm)
    onAction('등산 이력을 수정했어요.', () =>
      request(`/api/hiking-records/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }),
    )
    setEditingRecordId(null)
    setEditForm(null)
  }

  const deleteRecord = (id) =>
    onAction('등산 이력을 삭제했어요.', () =>
      request(`/api/hiking-records/${id}`, {
        method: 'DELETE',
      }),
    )

  return (
    <section className="hiking-layout">
      <datalist id="previous-hiking-mountains">
        {previousMountains.map((record) => (
          <option key={record.id} value={record.mountainName} />
        ))}
      </datalist>
      <div className="hiking-left-column">
        <div className="hiking-summary-panel">
          <div className="section-title">
            <h2>우리의 등산 현황</h2>
            <span>{filteredRecords.length}회</span>
          </div>
          <div className="hiking-stats">
            <div>
              <span>정상 고도 합계</span>
              <strong>{totalElevation.toLocaleString('ko-KR')}m</strong>
            </div>
            <div>
              <span>정복한 산 개수</span>
              <strong>{uniqueMountainCount.toLocaleString('ko-KR')}개</strong>
            </div>
            <div>
              <span>BAC 명산100</span>
              <strong>{completedBacIds.size}/{bacMountains.length}</strong>
            </div>
          </div>
          <div className="hiking-period-filter">
            <div className="hiking-period-toolbar">
              <span className={hasRangeSelection ? 'hiking-period-status custom' : 'hiking-period-status'}>
                {hasRangeSelection ? '선택기간' : '전체기간'}
              </span>
              <button
                type="button"
                className="ghost-button hiking-period-reset"
                disabled={!hasRangeSelection}
                onClick={() => setRange({ start: emptyDateParts(), end: emptyDateParts() })}
              >
                <RotateCcw size={14} />
                <span>전체기간</span>
              </button>
            </div>
            <div className="hiking-period-grid">
              <HikingDateSelect
                label="시작일"
                value={range.start}
                years={filterYears}
                onChange={(start) => setRange((current) => ({ ...current, start }))}
              />
              <HikingDateSelect
                label="종료일"
                value={range.end}
                years={filterYears}
                onChange={(end) => setRange((current) => ({ ...current, end }))}
              />
            </div>
          </div>
        </div>

        <div className={`hiking-map-panel hiking-collapsible-panel${isMapExpanded ? '' : ' collapsed'}`}>
          <div className="section-title">
            <h2>등산 지도</h2>
            <div className="section-actions">
              <span>{filteredRecords.filter(hasValidCoordinates).length}개</span>
              <button
                type="button"
                className="icon-only hiking-collapse-toggle"
                onClick={() => setIsMapExpanded((expanded) => !expanded)}
                aria-expanded={isMapExpanded}
                aria-label={isMapExpanded ? '등산 지도 접기' : '등산 지도 펼치기'}
              >
                {isMapExpanded ? <ChevronUp size={17} /> : <ChevronDown size={17} />}
              </button>
            </div>
          </div>
          {isMapExpanded && (
            <div className="hiking-collapsible-content">
              <GoogleMountainMap records={filteredRecords} previewRecord={customPreviewRecord} />
            </div>
          )}
        </div>

        <div className={`hiking-record-panel hiking-collapsible-panel${isHistoryExpanded ? '' : ' collapsed'}`}>
          <div className="section-title">
            <h2>등산 이력</h2>
            <div className="section-actions">
              <span>
                {filteredRecords.length.toLocaleString('ko-KR')}회 · {totalElevation.toLocaleString('ko-KR')}m
              </span>
              <button
                type="button"
                className="icon-only"
                onClick={() => setIsHikingFormOpen(true)}
                aria-label="등반 추가"
              >
                <Plus size={17} />
              </button>
              <button
                type="button"
                className="icon-only hiking-collapse-toggle"
                onClick={() => setIsHistoryExpanded((expanded) => !expanded)}
                aria-expanded={isHistoryExpanded}
                aria-label={isHistoryExpanded ? '등산 이력 접기' : '등산 이력 펼치기'}
              >
                {isHistoryExpanded ? <ChevronUp size={17} /> : <ChevronDown size={17} />}
              </button>
            </div>
          </div>
          {isHistoryExpanded && (
            <div className="hiking-record-list hiking-collapsible-content">
            {filteredRecords.length === 0 ? (
              <div className="empty-state">
                <Mountain size={28} />
                <span>해당 기간의 등산 이력이 없어요.</span>
              </div>
            ) : (
              filteredRecords.map((record) => (
                <article key={record.id} className="hiking-record-row">
                  {editingRecordId === record.id && editForm ? (
                    <form className="hiking-edit-form" onSubmit={submitEditRecord}>
                      <div className="form-grid two compact">
                        <label>
                          산 이름
                          <input
                            required
                            list="previous-hiking-mountains"
                            value={editForm.mountainName}
                            onChange={(event) =>
                              setEditForm(
                                applyPreviousMountain(event.target.value, {
                                  ...editForm,
                                  mountainName: event.target.value,
                                }),
                              )
                            }
                          />
                        </label>
                        <label>
                          정상 높이
                          <input
                            required
                            type="number"
                            min="1"
                            max="3000"
                            value={editForm.elevationMeter}
                            onChange={(event) => setEditForm({ ...editForm, elevationMeter: event.target.value })}
                          />
                        </label>
                        <label>
                          위치
                          <input
                            required
                            value={editForm.location}
                            onChange={(event) => setEditForm({ ...editForm, location: event.target.value })}
                          />
                        </label>
                        <label>
                          등반일
                          <input
                            required
                            type="date"
                            value={editForm.climbedAt}
                            onChange={(event) => setEditForm({ ...editForm, climbedAt: event.target.value })}
                          />
                        </label>
                        <label>
                          위도
                          <input
                            type="number"
                            step="0.000001"
                            min="-90"
                            max="90"
                            value={editForm.latitude}
                            onChange={(event) => setEditForm({ ...editForm, latitude: event.target.value })}
                          />
                        </label>
                        <label>
                          경도
                          <input
                            type="number"
                            step="0.000001"
                            min="-180"
                            max="180"
                            value={editForm.longitude}
                            onChange={(event) => setEditForm({ ...editForm, longitude: event.target.value })}
                          />
                        </label>
                      </div>
                      <label>
                        메모
                        <textarea
                          rows="2"
                          value={editForm.memo}
                          onChange={(event) => setEditForm({ ...editForm, memo: event.target.value })}
                        />
                      </label>
                      <div className="record-actions">
                        <button type="submit" className="primary-button">
                          <Pencil size={16} />
                          <span>저장</span>
                        </button>
                        <button
                          type="button"
                          className="ghost-button"
                          onClick={() => {
                            setEditingRecordId(null)
                            setEditForm(null)
                          }}
                        >
                          <X size={16} />
                          <span>취소</span>
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <div>
                        <div className="record-title-line">
                          <strong>{record.mountainName}</strong>
                          {isBacRecord(record) && <span className="bac-record-tag">BAC 명산100</span>}
                        </div>
                        <span className={authorBadgeClass(record)}>{authorNickname(record)}</span>
                        <span>{formatDate(record.climbedAt)}</span>
                        <span>{record.location}</span>
                        {record.memo && <p>{record.memo}</p>}
                      </div>
                      <div className="record-side">
                        <em>{record.elevationMeter.toLocaleString('ko-KR')}m</em>
                        <button type="button" className="icon-only" onClick={() => startEditRecord(record)}>
                          <Pencil size={16} />
                        </button>
                        <button type="button" className="icon-only danger" onClick={() => deleteRecord(record.id)}>
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </>
                  )}
                </article>
              ))
            )}
            </div>
          )}
        </div>
      </div>

      <div className="hiking-right-column">
        {isHikingFormOpen && (
          <div className="form-modal-backdrop" onClick={() => setIsHikingFormOpen(false)}>
            <form
              className="form-modal stack-form hiking-form-modal"
              onSubmit={submitRecord}
              onClick={(event) => event.stopPropagation()}
            >
        <div className="section-title">
          <h2>등산 이력 추가</h2>
          <button
            type="button"
            className="modal-close-button"
            onClick={() => setIsHikingFormOpen(false)}
            aria-label="닫기"
          >
            <X size={20} />
          </button>
        </div>
        <div className="segmented entry-mode-options">
          {[
            ['bac', 'BAC 명산100'],
            ['custom', '직접 입력'],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              className={entryMode === value ? 'active' : ''}
              onClick={() => setEntryMode(value)}
            >
              <span>{label}</span>
            </button>
          ))}
        </div>
        {entryMode === 'bac' ? (
          <>
            <label>
              산 선택
              <select value={selectedMountainId} onChange={(event) => setSelectedMountainId(event.target.value)}>
                {bacMountains.map((mountain) => (
                  <option key={mountain.id} value={mountain.id}>
                    {mountain.name} · {mountain.elevationMeter}m
                  </option>
                ))}
              </select>
            </label>
            <div className="mountain-selected-card">
              <strong>{selectedMountain.name}</strong>
              <span>{selectedMountain.location}</span>
              <span>{selectedMountain.elevationMeter.toLocaleString('ko-KR')}m</span>
            </div>
          </>
        ) : (
          <div className="form-grid two">
            <label>
              산 이름
              <input
                required
                list="previous-hiking-mountains"
                value={customMountain.mountainName}
                onChange={(event) =>
                  setCustomMountain(
                    applyPreviousMountain(event.target.value, {
                      ...customMountain,
                      mountainName: event.target.value,
                    }),
                  )
                }
                placeholder="예: 청계산"
              />
            </label>
            <label>
              정상 높이
              <input
                required
                type="number"
                min="1"
                max="3000"
                value={customMountain.elevationMeter}
                onChange={(event) =>
                  setCustomMountain({ ...customMountain, elevationMeter: event.target.value })
                }
                placeholder="m"
              />
            </label>
            <label>
              위치
              <input
                required
                value={customMountain.location}
                onChange={(event) =>
                  setCustomMountain({ ...customMountain, location: event.target.value })
                }
                placeholder="예: 서울 서초구"
              />
            </label>
            <label>
              위도
              <input
                type="number"
                step="0.000001"
                min="-90"
                max="90"
                value={customMountain.latitude}
                onChange={(event) =>
                  setCustomMountain({ ...customMountain, latitude: event.target.value })
                }
                placeholder="선택"
              />
            </label>
            <label>
              경도
              <input
                type="number"
                step="0.000001"
                min="-180"
                max="180"
                value={customMountain.longitude}
                onChange={(event) =>
                  setCustomMountain({ ...customMountain, longitude: event.target.value })
                }
                placeholder="선택"
              />
            </label>
          </div>
        )}
        <label>
          등반일
          <input type="date" value={climbedAt} onChange={(event) => setClimbedAt(event.target.value)} />
        </label>
        <label>
          메모
          <textarea rows="3" value={memo} onChange={(event) => setMemo(event.target.value)} />
        </label>
        <button type="submit" className="primary-button">
          <Plus size={17} />
          <span>추가</span>
        </button>
            </form>
          </div>
        )}

      <div className="bac-panel">
        <div className="section-title">
          <h2>BAC 명산100</h2>
          <span>{bacMountains.length}개</span>
        </div>
        <input
          className="mountain-search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="산 이름 또는 지역 검색"
        />
        <div className="mountain-list">
          {filteredMountains.map((mountain) => (
            <button
              key={mountain.id}
              type="button"
              className={mountain.id === selectedMountainId ? 'mountain-row active' : 'mountain-row'}
              onClick={() => setSelectedMountainId(mountain.id)}
            >
              <strong>{mountain.name}</strong>
              <span>{mountain.location}</span>
              <em>{mountain.elevationMeter.toLocaleString('ko-KR')}m</em>
            </button>
          ))}
        </div>
      </div>
      </div>
    </section>
  )
}

function FoodieView({ events, places, currentUser, onAction }) {
  const [keyword, setKeyword] = useState('')
  const [foodCategoryFilter, setFoodCategoryFilter] = useState('all')
  const [searchResults, setSearchResults] = useState([])
  const [selectedPlace, setSelectedPlace] = useState(null)
  const [searchStatus, setSearchStatus] = useState('')
  const [currentLocation, setCurrentLocation] = useState(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [form, setForm] = useState(null)
  const togetherEvents = useMemo(
    () => events.filter((event) => event.category === 'together').sort(compareEventsByDateDesc),
    [events],
  )

  const searchFoodPlaces = async (event) => {
    event.preventDefault()
    if (!keyword.trim() && foodCategoryFilter === 'all') {
      setSearchStatus('검색어를 입력하거나 식당 종류를 선택해줘요.')
      return
    }
    setSearchStatus('검색 중이에요.')
    try {
      const maps = await loadKakaoMaps(KAKAO_MAPS_API_KEY)
      const service = new maps.services.Places()
      const searchOptions = currentLocation
        ? { location: new maps.LatLng(currentLocation.lat, currentLocation.lng), radius: 20000 }
        : undefined
      const query =
        foodCategoryFilter !== 'all'
          ? [keyword.trim(), foodCategoryFilter].filter(Boolean).join(' ')
          : keyword.trim()
      service.keywordSearch(
        query,
        (data, status) => {
          if (status === maps.services.Status.OK) {
            const filteredData = filterFoodiePlacesByCategory(data, foodCategoryFilter)
            setSearchResults(filteredData)
            setSelectedPlace(filteredData[0] || null)
            setSearchStatus(
              filteredData.length
                ? `${filteredData.length.toLocaleString('ko-KR')}개의 장소를 찾았어요.`
                : '선택한 종류에 맞는 장소가 없어요.',
            )
          } else if (status === maps.services.Status.ZERO_RESULT) {
            setSearchResults([])
            setSelectedPlace(null)
            setSearchStatus('검색 결과가 없어요.')
          } else {
            setSearchStatus('카카오맵 검색을 불러오지 못했어요.')
          }
        },
        searchOptions,
      )
    } catch {
      setSearchStatus('카카오맵 API 키를 설정하면 맛집을 검색할 수 있어요.')
    }
  }

  const openCreateForm = (place) => {
    setSelectedPlace(place)
    setForm(kakaoPlaceToFoodieForm(place, currentUser))
    setIsFormOpen(true)
  }

  const resetFoodieSearch = () => {
    setKeyword('')
    setFoodCategoryFilter('all')
    setSearchResults([])
    setSelectedPlace(null)
    setSearchStatus('')
  }

  const selectPlaceFromMap = useCallback((place) => {
    setSelectedPlace(place)
    if (place) {
      setSearchStatus('지도에서 선택한 카카오 장소예요. 맛집 추가 버튼을 눌러 기록할 수 있어요.')
    } else {
      setSearchStatus('근처에 선택할 수 있는 음식점이나 카페가 없어요.')
    }
  }, [])

  const openEditForm = (place) => {
    setSelectedPlace({
      id: place.kakaoPlaceId,
      place_name: place.name,
      category_name: place.categoryName,
      address_name: place.addressName,
      road_address_name: place.roadAddressName,
      phone: place.phone,
      place_url: place.placeUrl,
      y: place.latitude,
      x: place.longitude,
      rating: place.kakaoRating,
      review_count: place.kakaoReviewCount,
    })
    setForm({
      id: place.id,
      kakaoPlaceId: place.kakaoPlaceId || '',
      name: place.name || '',
      categoryName: place.categoryName || '',
      addressName: place.addressName || '',
      roadAddressName: place.roadAddressName || '',
      phone: place.phone || '',
      placeUrl: place.placeUrl || '',
      latitude: place.latitude ?? '',
      longitude: place.longitude ?? '',
      kakaoRating: place.kakaoRating ?? '',
      kakaoReviewCount: place.kakaoReviewCount ?? '',
      eventId: place.eventId || '',
      visitDate: place.visitDate || toDateInput(todayInKorea()),
      oneLineReview: place.oneLineReview || '',
      junhongRating: place.junhongRating ?? '0',
      sominRating: place.sominRating ?? '0',
      junhongReview: place.junhongReview || '',
      sominReview: place.sominReview || '',
      photoFile: null,
      createdBy: place.createdBy || currentUser.username,
      creatorNickname: place.creatorNickname || currentUser.nickname,
    })
    setIsFormOpen(true)
  }

  const submitFoodiePlace = async (event) => {
    event.preventDefault()
    if (!form) return
    const formData = new FormData()
    const fields = {
      kakaoPlaceId: form.kakaoPlaceId,
      name: form.name,
      categoryName: form.categoryName,
      addressName: form.addressName,
      roadAddressName: form.roadAddressName,
      phone: form.phone,
      placeUrl: form.placeUrl,
      latitude: form.latitude,
      longitude: form.longitude,
      kakaoRating: form.kakaoRating,
      kakaoReviewCount: form.kakaoReviewCount,
      eventId: form.eventId,
      visitDate: form.visitDate,
      oneLineReview: form.oneLineReview,
      junhongRating: form.junhongRating,
      sominRating: form.sominRating,
      junhongReview: form.junhongReview,
      sominReview: form.sominReview,
      createdBy: form.createdBy || currentUser.username,
      creatorNickname: form.creatorNickname || currentUser.nickname,
    }
    Object.entries(fields).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        formData.append(key, value)
      }
    })
    if (form.photoFile) {
      formData.append('photo', await normalizePhotoForUpload(form.photoFile))
    }
    const isEdit = Boolean(form.id)
    const saved = await onAction(isEdit ? '맛집 기록을 수정했어요.' : '우리 맛집 리스트에 추가했어요.', () =>
      request(isEdit ? `/api/foodie-places/${form.id}` : '/api/foodie-places', {
        method: isEdit ? 'PUT' : 'POST',
        body: formData,
      }),
    )
    if (saved) {
      setIsFormOpen(false)
      setForm(null)
    }
  }

  const deleteFoodiePlace = (place) =>
    onAction('맛집 기록을 삭제했어요.', () =>
      request(`/api/foodie-places/${place.id}`, {
        method: 'DELETE',
      }),
    )

  const applyEventToForm = (eventId) => {
    const matchedEvent = togetherEvents.find((event) => event.id === eventId)
    setForm({
      ...form,
      eventId,
      visitDate: matchedEvent?.date || form.visitDate,
    })
  }

  return (
    <section className="foodie-layout">
      <div className="foodie-map-panel">
        <div className="section-title">
          <h2>맛집 지도</h2>
          <span>{currentLocation ? '현위치 기준' : '위치 확인 중'}</span>
        </div>
        <div className="foodie-map-shell">
          <KakaoFoodieMap
            places={places}
            searchResults={searchResults}
            selectedPlace={selectedPlace}
            currentLocation={currentLocation}
            onLocationDetected={setCurrentLocation}
            onPlaceSelected={selectPlaceFromMap}
          />
          <div className="foodie-map-search">
            <form className="foodie-search-form" onSubmit={searchFoodPlaces}>
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="식당, 카페, 지역을 검색해요"
              />
              <select
                value={foodCategoryFilter}
                onChange={(event) => setFoodCategoryFilter(event.target.value)}
                aria-label="식당 종류"
              >
                {FOODIE_CATEGORY_FILTERS.map((filter) => (
                  <option key={filter.value} value={filter.value}>
                    {filter.label}
                  </option>
                ))}
              </select>
              <button type="submit" className="primary-button">
                <Search size={17} />
                <span>검색</span>
              </button>
              <button type="button" className="secondary-button" onClick={resetFoodieSearch}>
                <RotateCcw size={16} />
                <span>리셋</span>
              </button>
            </form>
            {searchStatus && <p className="foodie-search-status">{searchStatus}</p>}
            <div className="foodie-search-results">
              {searchResults.map((place) => (
                <button
                  key={place.id}
                  type="button"
                  className={selectedPlace?.id === place.id ? 'foodie-search-row active' : 'foodie-search-row'}
                  onClick={() => setSelectedPlace(place)}
                >
                  <strong>{place.place_name}</strong>
                  <span>
                    {compactFoodCategory(place.category_name)} · {place.road_address_name || place.address_name}
                  </span>
                </button>
              ))}
            </div>
            <button
              type="button"
              className="primary-button foodie-add-button"
              onClick={() => selectedPlace && openCreateForm(selectedPlace)}
              disabled={!selectedPlace}
            >
              <Plus size={17} />
              <span>선택 맛집 추가</span>
            </button>
          </div>
        </div>
      </div>

      <div className="foodie-list-panel">
        <div className="section-title">
          <h2>맛집 리스트</h2>
          <span>{places.length}곳</span>
        </div>
        <div className="foodie-place-list">
          {places.length === 0 ? (
            <div className="empty-state">
              <Utensils size={28} />
              <span>우리만의 맛집을 추가해보자.</span>
            </div>
          ) : (
            places.map((place) => (
              <article key={place.id} className="foodie-place-card">
                <div className="foodie-photo">
                  {place.photoUrl ? <img src={place.photoUrl} alt={place.name} /> : <Utensils size={28} />}
                </div>
                <div className="foodie-card-main">
                  <div className="record-title-line">
                    <strong>{place.name}</strong>
                    <span className={authorBadgeClass(place)}>{authorNickname(place)}</span>
                  </div>
                  <span>{compactFoodCategory(place.categoryName)} · {place.roadAddressName || place.addressName}</span>
                  <span>
                    {place.visitDate ? formatDate(place.visitDate) : '방문일 미정'}
                    {place.eventTitle ? ` · ${place.eventTitle}` : ''}
                  </span>
                  <div className="foodie-rating-line">
                    <span>준홍 {userRatingLabel(place.junhongRating)}</span>
                    <span>소민 {userRatingLabel(place.sominRating)}</span>
                  </div>
                  <div className="foodie-review-line">
                    {(place.junhongReview || place.oneLineReview) && (
                      <p>
                        <strong>준홍</strong>
                        <span>{place.junhongReview || place.oneLineReview}</span>
                      </p>
                    )}
                    {place.sominReview && (
                      <p>
                        <strong>소민</strong>
                        <span>{place.sominReview}</span>
                      </p>
                    )}
                  </div>
                </div>
                <div className="record-side foodie-card-actions">
                  <button type="button" className="icon-only" onClick={() => openEditForm(place)}>
                    <Pencil size={16} />
                  </button>
                  <button type="button" className="icon-only danger" onClick={() => deleteFoodiePlace(place)}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </article>
            ))
          )}
        </div>
      </div>

      {isFormOpen && form && (
        <div className="form-modal-backdrop" onClick={() => setIsFormOpen(false)}>
          <form
            className="form-modal stack-form foodie-form-modal"
            onSubmit={submitFoodiePlace}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="section-title">
              <h2>{form.id ? '맛집 수정' : '맛집 추가'}</h2>
              <button type="button" className="modal-close-button" onClick={() => setIsFormOpen(false)} aria-label="닫기">
                <X size={20} />
              </button>
            </div>
            <div className="foodie-selected-card">
              <strong>{form.name}</strong>
              <span>{compactFoodCategory(form.categoryName)} · {form.roadAddressName || form.addressName}</span>
            </div>
            <div className="form-grid two compact">
              <label>
                맛집 이름
                <input
                  required
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                  placeholder="맛집 이름을 입력해요"
                />
              </label>
              <label>
                카테고리
                <input
                  value={form.categoryName}
                  onChange={(event) => setForm({ ...form, categoryName: event.target.value })}
                  placeholder="한식, 카페, 중식..."
                />
              </label>
            </div>
            <label>
              주소
              <input
                value={form.roadAddressName || form.addressName}
                onChange={(event) =>
                  setForm({
                    ...form,
                    roadAddressName: event.target.value,
                    addressName: event.target.value,
                  })
                }
                placeholder="지도에서 선택한 주소를 확인해요"
              />
            </label>
            <label>
              같이 일정 연결
              <select value={form.eventId} onChange={(event) => applyEventToForm(event.target.value)}>
                <option value="">일정 없이 등록</option>
                {togetherEvents.map((event) => (
                  <option key={event.id} value={event.id}>
                    {event.date}
                    {event.endDate && event.endDate !== event.date ? `~${event.endDate}` : ''} · {event.title}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-grid two compact">
              <label>
                방문날짜
                <input
                  type="date"
                  value={form.visitDate}
                  onChange={(event) => setForm({ ...form, visitDate: event.target.value })}
                />
              </label>
              <label>
                사진
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => setForm({ ...form, photoFile: event.target.files?.[0] || null })}
                />
              </label>
            </div>
            <div className="foodie-review-form-grid">
              <section className="foodie-person-review">
                <div className="foodie-person-review-header">
                  <strong>준홍</strong>
                  <span>{Number(form.junhongRating || 0).toFixed(1)}점</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="5"
                  step="0.5"
                  value={form.junhongRating === '' ? 0 : form.junhongRating}
                  onChange={(event) => setForm({ ...form, junhongRating: event.target.value })}
                />
                <label>
                  준홍 한줄평
                  <textarea
                    rows="3"
                    value={form.junhongReview}
                    onChange={(event) => setForm({ ...form, junhongReview: event.target.value })}
                    placeholder="준홍의 맛 포인트를 남겨요"
                  />
                </label>
              </section>
              <section className="foodie-person-review">
                <div className="foodie-person-review-header">
                  <strong>소민</strong>
                  <span>{Number(form.sominRating || 0).toFixed(1)}점</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="5"
                  step="0.5"
                  value={form.sominRating === '' ? 0 : form.sominRating}
                  onChange={(event) => setForm({ ...form, sominRating: event.target.value })}
                />
                <label>
                  소민 한줄평
                  <textarea
                    rows="3"
                    value={form.sominReview}
                    onChange={(event) => setForm({ ...form, sominReview: event.target.value })}
                    placeholder="소민의 맛 포인트를 남겨요"
                  />
                </label>
              </section>
            </div>
            <button type="submit" className="primary-button">
              {form.id ? <Pencil size={17} /> : <Plus size={17} />}
              <span>{form.id ? '저장' : '추가'}</span>
            </button>
          </form>
        </div>
      )}
    </section>
  )
}

function compactFoodCategory(value = '') {
  if (!value) return '분류 미정'
  const parts = value.split('>').map((part) => part.trim()).filter(Boolean)
  return parts.at(-1) || value
}

function GoogleMountainMap({ records, previewRecord = null }) {
  const markers = useMemo(() => {
    const savedMarkers = records
      .map((record) => {
        const officialMountain = findBacMountainForRecord(record)
        const shouldUseOfficialCoordinates =
          record.source !== 'custom' &&
          officialMountain &&
          (!hasValidCoordinates(record) || isLikelyLegacyEstimatedCoordinate(record))

        return shouldUseOfficialCoordinates
          ? {
              ...record,
              latitude: officialMountain.latitude,
              longitude: officialMountain.longitude,
            }
          : record
      })
      .filter(hasValidCoordinates)

    return previewRecord ? [...savedMarkers, previewRecord] : savedMarkers
  }, [records, previewRecord])
  const mapRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRefs = useRef([])
  const [mapError, setMapError] = useState('')
  const missingKeyMessage = !GOOGLE_MAPS_API_KEY
    ? 'Google Maps API 키를 설정하면 지도를 움직이면서 마커를 확인할 수 있어요.'
    : ''

  useEffect(() => {
    if (!GOOGLE_MAPS_API_KEY) {
      return
    }

    let cancelled = false
    loadGoogleMaps(GOOGLE_MAPS_API_KEY)
      .then((maps) => {
        if (cancelled || !mapRef.current) return

        if (!mapInstanceRef.current) {
          mapInstanceRef.current = new maps.Map(mapRef.current, {
            center: { lat: 36.45, lng: 127.85 },
            zoom: 7,
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: false,
            gestureHandling: 'greedy',
          })
        }

        markerRefs.current.forEach((marker) => marker.setMap(null))
        markerRefs.current = markers.map((record) => {
          const marker = new maps.Marker({
            position: { lat: Number(record.latitude), lng: Number(record.longitude) },
            map: mapInstanceRef.current,
            title: `${record.mountainName} · ${record.elevationMeter}m`,
            label: record.isPreview ? { text: '입력', color: '#ffffff', fontWeight: '700' } : undefined,
          })
          const infoWindow = new maps.InfoWindow({
            content: `<strong>${escapeHtml(record.mountainName)}</strong><br>${escapeHtml(record.location)}<br>${Number(record.elevationMeter).toLocaleString('ko-KR')}m`,
          })
          marker.addListener('click', () => infoWindow.open(mapInstanceRef.current, marker))
          return marker
        })

        if (markers.length > 0) {
          const bounds = new maps.LatLngBounds()
          markers.forEach((record) => bounds.extend({ lat: Number(record.latitude), lng: Number(record.longitude) }))
          mapInstanceRef.current.fitBounds(bounds)
          if (markers.length === 1) {
            mapInstanceRef.current.setZoom(10)
          }
        }
      })
      .catch(() => {
        if (!cancelled) setMapError('Google Maps를 불러오지 못했어요. API 키와 결제 설정을 확인해주세요.')
      })

    return () => {
      cancelled = true
    }
  }, [markers])

  return (
    <div className="google-map-panel">
      <div ref={mapRef} className="google-map-canvas" />
      {(missingKeyMessage || mapError) && (
        <div className="map-key-message">
          <MapPin size={20} />
          <span>{missingKeyMessage || mapError}</span>
        </div>
      )}
    </div>
  )
}

function KakaoFoodieMap({
  places,
  searchResults = [],
  selectedPlace = null,
  currentLocation = null,
  onLocationDetected,
  onPlaceSelected,
}) {
  const mapRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRefs = useRef([])
  const currentMarkerRef = useRef(null)
  const mapClickListenerRef = useRef(null)
  const activeInfoWindowRef = useRef(null)
  const activeInfoMarkerIdRef = useRef(null)
  const hasCenteredOnLocationRef = useRef(false)
  const [mapError, setMapError] = useState('')
  const missingKeyMessage = !KAKAO_MAPS_API_KEY
    ? 'Kakao Maps API 키를 설정하면 맛집 검색과 지도를 사용할 수 있어요.'
    : ''
  const markers = useMemo(() => {
    const savedMarkers = places
      .filter((place) => Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude)))
      .map((place) => ({
        id: `saved-${place.id}`,
        name: place.name,
        categoryName: place.categoryName,
        addressName: place.roadAddressName || place.addressName,
        latitude: Number(place.latitude),
        longitude: Number(place.longitude),
        kind: 'saved',
      }))
    const searchMarkers = searchResults
      .filter((place) => Number.isFinite(Number(place.y)) && Number.isFinite(Number(place.x)))
      .map((place) => ({
        id: `search-${place.id}`,
        name: place.place_name,
        categoryName: place.category_name,
        addressName: place.road_address_name || place.address_name,
        latitude: Number(place.y),
        longitude: Number(place.x),
        kind: selectedPlace?.id === place.id ? 'selected' : 'search',
      }))
    const selectedMarker =
      selectedPlace &&
      Number.isFinite(Number(selectedPlace.y)) &&
      Number.isFinite(Number(selectedPlace.x)) &&
      !searchResults.some((place) => place.id === selectedPlace.id)
        ? [
            {
              id: `selected-${selectedPlace.id}`,
              name: selectedPlace.place_name,
              categoryName: selectedPlace.category_name,
              addressName: selectedPlace.road_address_name || selectedPlace.address_name,
              latitude: Number(selectedPlace.y),
              longitude: Number(selectedPlace.x),
              kind: 'selected',
            },
          ]
        : []
    return [...savedMarkers, ...searchMarkers, ...selectedMarker]
  }, [places, searchResults, selectedPlace])

  useEffect(() => {
    if (!navigator.geolocation || currentLocation) return
    navigator.geolocation.getCurrentPosition(
      (position) => {
        onLocationDetected?.({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        })
      },
      () => {
        setMapError('현재 위치 권한을 허용하면 내 주변 기준으로 맛집 지도를 볼 수 있어요.')
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    )
  }, [currentLocation, onLocationDetected])

  useEffect(() => {
    if (!KAKAO_MAPS_API_KEY) return
    let cancelled = false
    loadKakaoMaps(KAKAO_MAPS_API_KEY)
      .then((maps) => {
        if (cancelled || !mapRef.current) return
        if (!mapInstanceRef.current) {
          mapInstanceRef.current = new maps.Map(mapRef.current, {
            center: currentLocation
              ? new maps.LatLng(currentLocation.lat, currentLocation.lng)
              : new maps.LatLng(37.5665, 126.978),
            level: currentLocation ? 4 : 6,
          })
        }

        if (currentMarkerRef.current) {
          currentMarkerRef.current.setMap(null)
          currentMarkerRef.current = null
        }
        if (currentLocation) {
          currentMarkerRef.current = new maps.Marker({
            position: new maps.LatLng(currentLocation.lat, currentLocation.lng),
            map: mapInstanceRef.current,
            title: '현재 위치',
          })
          if (!hasCenteredOnLocationRef.current && markers.length === 0) {
            mapInstanceRef.current.setCenter(new maps.LatLng(currentLocation.lat, currentLocation.lng))
            mapInstanceRef.current.setLevel(4)
            hasCenteredOnLocationRef.current = true
          }
        }

        if (mapClickListenerRef.current) {
          maps.event.removeListener(mapClickListenerRef.current)
          mapClickListenerRef.current = null
        }
        if (onPlaceSelected) {
          const placeService = new maps.services.Places()
          mapClickListenerRef.current = maps.event.addListener(mapInstanceRef.current, 'click', (mouseEvent) => {
            const latlng = mouseEvent.latLng
            const lat = latlng.getLat()
            const lng = latlng.getLng()
            mapInstanceRef.current.setCenter(latlng)
            const clickPoint = { lat, lng }
            const options = {
              location: new maps.LatLng(lat, lng),
              radius: 120,
              sort: maps.services.SortBy.DISTANCE,
            }
            const searchCategory = (categoryCode) =>
              new Promise((resolve) => {
                placeService.categorySearch(
                  categoryCode,
                  (data, status) => resolve(status === maps.services.Status.OK ? data : []),
                  options,
                )
              })

            Promise.all([searchCategory('FD6'), searchCategory('CE7')]).then(([foodPlaces, cafePlaces]) => {
              const closestPlace = [...foodPlaces, ...cafePlaces]
                .filter((place) => Number.isFinite(Number(place.y)) && Number.isFinite(Number(place.x)))
                .sort((first, second) => kakaoDistanceFrom(clickPoint, first) - kakaoDistanceFrom(clickPoint, second))[0]
              onPlaceSelected(closestPlace || null)
            })
          })
        }

        if (activeInfoWindowRef.current) {
          activeInfoWindowRef.current.close()
          activeInfoWindowRef.current = null
          activeInfoMarkerIdRef.current = null
        }
        markerRefs.current.forEach((marker) => marker.setMap(null))
        const markerImages = {
          saved: createKakaoMarkerImage(maps, '#f59e0b'),
          search: createKakaoMarkerImage(maps, '#3b82f6'),
          selected: createKakaoMarkerImage(maps, '#d95f59'),
        }
        markerRefs.current = markers.map((place) => {
          const marker = new maps.Marker({
            position: new maps.LatLng(place.latitude, place.longitude),
            map: mapInstanceRef.current,
            image: markerImages[place.kind] || markerImages.search,
          })
          const infoWindow = new maps.InfoWindow({
            content: `<div style="padding:8px 10px;font-size:12px;line-height:1.5"><strong>${escapeHtml(place.name)}</strong><br>${escapeHtml(compactFoodCategory(place.categoryName))}<br>${escapeHtml(place.addressName)}</div>`,
          })
          maps.event.addListener(marker, 'click', () => {
            if (activeInfoMarkerIdRef.current === place.id) {
              infoWindow.close()
              activeInfoWindowRef.current = null
              activeInfoMarkerIdRef.current = null
              return
            }
            activeInfoWindowRef.current?.close()
            infoWindow.open(mapInstanceRef.current, marker)
            activeInfoWindowRef.current = infoWindow
            activeInfoMarkerIdRef.current = place.id
          })
          return marker
        })

        if (
          selectedPlace &&
          Number.isFinite(Number(selectedPlace.y)) &&
          Number.isFinite(Number(selectedPlace.x)) &&
          !searchResults.some((place) => place.id === selectedPlace.id)
        ) {
          mapInstanceRef.current.setCenter(new maps.LatLng(Number(selectedPlace.y), Number(selectedPlace.x)))
          mapInstanceRef.current.setLevel(4)
        } else if (markers.length > 0) {
          const bounds = new maps.LatLngBounds()
          markers.forEach((place) => bounds.extend(new maps.LatLng(place.latitude, place.longitude)))
          mapInstanceRef.current.setBounds(bounds)
          if (markers.length === 1) {
            mapInstanceRef.current.setLevel(4)
          }
        }
      })
      .catch(() => {
        if (!cancelled) setMapError('Kakao Maps를 불러오지 못했어요. JavaScript 키와 도메인 설정을 확인해주세요.')
      })

    return () => {
      cancelled = true
      if (window.kakao?.maps && mapClickListenerRef.current) {
        window.kakao.maps.event.removeListener(mapClickListenerRef.current)
        mapClickListenerRef.current = null
      }
    }
  }, [markers, currentLocation, onPlaceSelected, searchResults])

  return (
    <div className="kakao-map-panel">
      <div ref={mapRef} className="kakao-map-canvas" />
      {(missingKeyMessage || mapError) && (
        <div className="map-key-message">
          <MapPin size={20} />
          <span>{missingKeyMessage || mapError}</span>
        </div>
      )}
    </div>
  )
}

function MediaGrid({
  items,
  events = [],
  compact = false,
  horizontal = false,
  instagram = false,
  showCardActions = true,
  currentUser = DEFAULT_AUTHOR,
  openMediaId = null,
  onMediaDeepLinkOpened,
  onAction,
}) {
  const [selectedMedia, setSelectedMedia] = useState(null)
  const [editingMedia, setEditingMedia] = useState(null)
  const [comments, setComments] = useState([])
  const [commentText, setCommentText] = useState('')
  const [editingCommentId, setEditingCommentId] = useState(null)
  const [editingCommentText, setEditingCommentText] = useState('')
  const [swipeOffset, setSwipeOffset] = useState(0)
  const [isSwiping, setIsSwiping] = useState(false)
  const [slideAnimation, setSlideAnimation] = useState('')
  const swipeRef = useRef({ active: false, horizontal: false, pointerId: null, startX: 0, startY: 0 })
  const [editForm, setEditForm] = useState({
    eventId: '',
    title: '',
    memo: '',
    capturedAt: '',
  })

  useEffect(() => {
    preloadMediaComments().catch(() => {})
  }, [])

  useEffect(() => {
    if (!openMediaId) return
    const deepLinkedMedia = items.find((item) => item.id === openMediaId)
    if (!deepLinkedMedia) return
    setSelectedMedia(deepLinkedMedia)
    onMediaDeepLinkOpened?.()
  }, [items, openMediaId, onMediaDeepLinkOpened])

  useEffect(() => {
    if (!selectedMedia) {
      setComments([])
      setCommentText('')
      setEditingCommentId(null)
      setEditingCommentText('')
      return
    }

    const mediaId = selectedMedia.id
    const hasCachedSnapshot = mediaCommentsCacheLoaded
    setComments(mediaCommentsCache.get(mediaId) || [])
    setEditingCommentId(null)
    setEditingCommentText('')

    let cancelled = false
    const loadComments = async () => {
      try {
        let data
        if (hasCachedSnapshot) {
          data = await request(`/api/media/${mediaId}/comments`)
          mediaCommentsCache.set(mediaId, data)
        } else {
          try {
            await preloadMediaComments()
            data = mediaCommentsCache.get(mediaId) || []
          } catch {
            data = await request(`/api/media/${mediaId}/comments`)
            mediaCommentsCache.set(mediaId, data)
          }
        }

        if (!cancelled) setComments(data)
      } catch {
        if (!cancelled && !hasCachedSnapshot) setComments([])
      }
    }
    loadComments()

    return () => {
      cancelled = true
    }
  }, [selectedMedia])

  if (items.length === 0) {
    return (
      <div className="empty-state">
        <ImageIcon size={28} />
        <span>등록된 추억이 없어요.</span>
      </div>
    )
  }

  const toggleFavorite = (item) =>
    onAction(item.favorite ? '즐겨찾기를 해제했어요.' : '즐겨찾기에 추가했어요.', () =>
      request(`/api/media/${item.id}/favorite`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ favorite: !item.favorite }),
      }),
    )

  const deleteMedia = (item) =>
    onAction('앨범에서 추억을 삭제했어요.', () =>
      request(`/api/media/${item.id}`, {
        method: 'DELETE',
      }),
    )

  const openEditMedia = (item) => {
    setEditingMedia(item)
    setEditForm({
      eventId: item.eventId || '',
      title: item.title || '',
      memo: item.memo || '',
      capturedAt: item.capturedAt || '',
    })
  }

  const updateMedia = (event) => {
    event.preventDefault()
    if (!editingMedia) return
    const payload = {
      eventId: editForm.eventId || null,
      title: editForm.title,
      memo: editForm.memo,
      capturedAt: editForm.capturedAt || null,
    }
    setSelectedMedia(null)
    setEditingMedia(null)
    onAction('앨범 추억을 수정했어요.', () =>
      request(`/api/media/${editingMedia.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }),
    )
  }

  const rotateMedia = (item, rotationDegrees) => {
    setSelectedMedia(null)
    onAction('사진을 회전했어요.', () =>
      request(`/api/media/${item.id}/rotate`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rotationDegrees }),
      }),
    )
  }

  const selectedMediaIndex = selectedMedia
    ? items.findIndex((item) => item.id === selectedMedia.id)
    : -1
  const hasMediaCarousel = selectedMediaIndex >= 0 && items.length > 1
  const moveSelectedMedia = (amount) => {
    if (!hasMediaCarousel) return
    const nextIndex = (selectedMediaIndex + amount + items.length) % items.length
    setSwipeOffset(0)
    setIsSwiping(false)
    setSlideAnimation(amount > 0 ? 'slide-in-next' : 'slide-in-previous')
    setSelectedMedia(items[nextIndex])
  }

  const startMediaSwipe = (event) => {
    if (!hasMediaCarousel || (event.pointerType === 'mouse' && event.button !== 0)) return
    swipeRef.current = {
      active: true,
      horizontal: false,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
    }
  }

  const moveMediaSwipe = (event) => {
    const swipe = swipeRef.current
    if (!swipe.active || swipe.pointerId !== event.pointerId) return

    const deltaX = event.clientX - swipe.startX
    const deltaY = event.clientY - swipe.startY
    if (!swipe.horizontal) {
      if (Math.abs(deltaX) < 8 && Math.abs(deltaY) < 8) return
      if (Math.abs(deltaY) > Math.abs(deltaX)) {
        swipe.active = false
        return
      }
      swipe.horizontal = true
      event.currentTarget.setPointerCapture?.(event.pointerId)
      setSlideAnimation('')
      setIsSwiping(true)
    }

    event.preventDefault()
    const limit = event.currentTarget.clientWidth * 0.42
    setSwipeOffset(Math.max(-limit, Math.min(limit, deltaX)))
  }

  const finishMediaSwipe = (event) => {
    const swipe = swipeRef.current
    if (!swipe.active || swipe.pointerId !== event.pointerId) return

    const deltaX = event.clientX - swipe.startX
    const threshold = Math.min(96, Math.max(48, event.currentTarget.clientWidth * 0.14))
    if (swipe.horizontal && Math.abs(deltaX) >= threshold) {
      moveSelectedMedia(deltaX < 0 ? 1 : -1)
    } else {
      setSwipeOffset(0)
      setIsSwiping(false)
    }

    if (event.currentTarget.hasPointerCapture?.(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
    swipeRef.current = { active: false, horizontal: false, pointerId: null, startX: 0, startY: 0 }
  }

  const cancelMediaSwipe = (event) => {
    if (swipeRef.current.pointerId !== event.pointerId) return
    setSwipeOffset(0)
    setIsSwiping(false)
    swipeRef.current = { active: false, horizontal: false, pointerId: null, startX: 0, startY: 0 }
  }

  const submitComment = (event) => {
    event.preventDefault()
    const content = commentText.trim()
    if (!selectedMedia || !content) return

    let createdComment = null
    const mediaId = selectedMedia.id
    const author = currentUser || DEFAULT_AUTHOR

    onAction('댓글을 등록했어요.', async () => {
      createdComment = await request(`/api/media/${mediaId}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          content,
          createdBy: author.username,
          creatorNickname: author.nickname,
        }),
      })
      return createdComment
    }).then((success) => {
      if (!success || !createdComment) return
      setComments((current) => {
        const nextComments = [...current, createdComment]
        mediaCommentsCache.set(mediaId, nextComments)
        return nextComments
      })
      setCommentText('')
    })
  }

  const startEditComment = (comment) => {
    setEditingCommentId(comment.id)
    setEditingCommentText(comment.content)
  }

  const cancelEditComment = () => {
    setEditingCommentId(null)
    setEditingCommentText('')
  }

  const updateComment = (event) => {
    event.preventDefault()
    const content = editingCommentText.trim()
    if (!selectedMedia || !editingCommentId || !content) return

    let updatedComment = null
    const mediaId = selectedMedia.id
    const commentId = editingCommentId
    const author = currentUser || DEFAULT_AUTHOR

    onAction('댓글을 수정했어요.', async () => {
      updatedComment = await request(`/api/media/${mediaId}/comments/${commentId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          content,
          createdBy: author.username,
          creatorNickname: author.nickname,
        }),
      })
      return updatedComment
    }).then((success) => {
      if (!success || !updatedComment) return
      setComments((current) => {
        const nextComments = current.map((comment) =>
          comment.id === updatedComment.id ? updatedComment : comment,
        )
        mediaCommentsCache.set(mediaId, nextComments)
        return nextComments
      })
      cancelEditComment()
    })
  }

  const deleteComment = (comment) => {
    if (!selectedMedia) return

    const mediaId = selectedMedia.id
    onAction('댓글을 삭제했어요.', () =>
      request(`/api/media/${mediaId}/comments/${comment.id}`, {
        method: 'DELETE',
      }),
    ).then((success) => {
      if (!success) return
      setComments((current) => {
        const nextComments = current.filter((item) => item.id !== comment.id)
        mediaCommentsCache.set(mediaId, nextComments)
        return nextComments
      })
      if (editingCommentId === comment.id) cancelEditComment()
    })
  }

  return (
    <>
      <div
        className={[
          'media-grid',
          compact ? 'compact' : '',
          horizontal ? 'horizontal' : '',
          instagram ? 'instagram' : '',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        {items.map((item) => (
          <article key={item.id} className="media-card">
            <div className="media-preview">
              <button
                type="button"
                className="media-open-button"
                onClick={() => setSelectedMedia(item)}
                aria-label={`${item.title} 크게 보기`}
              >
                {item.mediaType === 'VIDEO' ? (
                  <video src={withCacheKey(item.url, mediaCacheKey(item))} muted />
                ) : (
                  <img src={withCacheKey(item.url, mediaCacheKey(item))} alt={item.title} />
                )}
              </button>
              <button
                type="button"
                className={item.favorite ? 'favorite-button active' : 'favorite-button'}
                onClick={() => toggleFavorite(item)}
                aria-label="즐겨찾기 전환"
              >
                <Heart size={15} fill={item.favorite ? 'currentColor' : 'none'} />
              </button>
            </div>
            <div className="media-body">
              <strong>{item.title}</strong>
              <span className={authorBadgeClass(item)}>{authorNickname(item)}</span>
              <span>{item.eventTitle || '별도 등록'}</span>
              <span>{formatDate(item.capturedAt || item.eventDate)}</span>
              {item.memo && <p>{item.memo}</p>}
            </div>
            {showCardActions && (
              <div className="media-actions">
                <button type="button" className="ghost-button" onClick={() => openEditMedia(item)}>
                  <Pencil size={15} />
                  <span>수정</span>
                </button>
                <button type="button" className="text-danger" onClick={() => deleteMedia(item)}>
                  <Trash2 size={15} />
                  <span>삭제</span>
                </button>
              </div>
            )}
          </article>
        ))}
      </div>

      {selectedMedia && (
        <div
          className="media-modal-backdrop"
          role="presentation"
          onClick={() => setSelectedMedia(null)}
        >
          <section
            className="media-modal"
            role="dialog"
            aria-modal="true"
            aria-label={`${selectedMedia.title} 크게 보기`}
            onClick={(event) => event.stopPropagation()}
            onKeyDown={(event) => {
              if (event.target instanceof HTMLElement && event.target.matches('input, textarea, select')) return
              if (event.key === 'ArrowLeft') moveSelectedMedia(-1)
              if (event.key === 'ArrowRight') moveSelectedMedia(1)
            }}
          >
            <button
              type="button"
              className="modal-close-button"
              onClick={() => setSelectedMedia(null)}
              aria-label="닫기"
            >
              <X size={20} />
            </button>
            {selectedMedia.mediaType === 'PHOTO' && (
              <div className="modal-rotate-actions">
                <button type="button" onClick={() => rotateMedia(selectedMedia, 270)}>
                  <RotateCcw size={17} />
                  <span>왼쪽 회전</span>
                </button>
                <button type="button" onClick={() => rotateMedia(selectedMedia, 90)}>
                  <RotateCw size={17} />
                  <span>오른쪽 회전</span>
                </button>
              </div>
            )}
            <div
              className="modal-media-stage"
              onPointerDown={startMediaSwipe}
              onPointerMove={moveMediaSwipe}
              onPointerUp={finishMediaSwipe}
              onPointerCancel={cancelMediaSwipe}
            >
              <div
                key={`media-${selectedMedia.id}`}
                className={[
                  'modal-media-slide',
                  isSwiping ? 'dragging' : '',
                  slideAnimation,
                ]
                  .filter(Boolean)
                  .join(' ')}
                style={{ '--swipe-offset': `${swipeOffset}px` }}
                onAnimationEnd={() => setSlideAnimation('')}
              >
                {selectedMedia.mediaType === 'VIDEO' ? (
                  <video
                    src={withCacheKey(selectedMedia.url, mediaCacheKey(selectedMedia))}
                    controls
                    controlsList="nodownload noplaybackrate"
                    autoPlay
                    onRateChange={(event) => {
                      if (event.currentTarget.playbackRate !== 1) {
                        event.currentTarget.playbackRate = 1
                      }
                    }}
                  />
                ) : (
                  <img
                    src={withCacheKey(selectedMedia.url, mediaCacheKey(selectedMedia))}
                    alt={selectedMedia.title}
                    draggable="false"
                  />
                )}
              </div>
              {hasMediaCarousel && (
                <div key={`cue-${selectedMedia.id}`} className="modal-swipe-cue" aria-hidden="true">
                  <ChevronLeft size={14} />
                  <span />
                  <ChevronRight size={14} />
                </div>
              )}
            </div>
            <div className="modal-media-info">
              <strong>{selectedMedia.title}</strong>
              <span className={authorBadgeClass(selectedMedia)}>{authorNickname(selectedMedia)}</span>
              <span>{selectedMedia.eventTitle || '별도 등록'}</span>
              <span>{formatDate(selectedMedia.capturedAt || selectedMedia.eventDate)}</span>
              {selectedMedia.memo && <p>{selectedMedia.memo}</p>}
              <div className="media-comments">
                  <div className="media-comments-list">
                    {comments.length > 0 ? (
                      comments.map((comment) => {
                        const isEditingComment = editingCommentId === comment.id

                        return (
                          <article key={comment.id} className="media-comment">
                            <div className="media-comment-header">
                              <div className="media-comment-meta">
                                <span className={authorBadgeClass(comment)}>{authorNickname(comment)}</span>
                                <time>{formatDateTime(comment.createdAt)}</time>
                              </div>
                              <div className="media-comment-actions">
                                <button type="button" onClick={() => startEditComment(comment)}>
                                  수정
                                </button>
                                <button type="button" onClick={() => deleteComment(comment)}>
                                  삭제
                                </button>
                              </div>
                            </div>
                            {isEditingComment ? (
                              <form className="comment-edit-form" onSubmit={updateComment}>
                                <input
                                  value={editingCommentText}
                                  onChange={(event) => setEditingCommentText(event.target.value)}
                                  maxLength={500}
                                  autoFocus
                                />
                                <div>
                                  <button type="submit" className="ghost-button" disabled={!editingCommentText.trim()}>
                                    저장
                                  </button>
                                  <button type="button" className="ghost-button" onClick={cancelEditComment}>
                                    취소
                                  </button>
                                </div>
                              </form>
                            ) : (
                              <p>{comment.content}</p>
                            )}
                          </article>
                        )
                      })
                    ) : (
                      <p className="comment-empty">아직 댓글이 없어요.</p>
                    )}
                  </div>
                  <form className="comment-form" onSubmit={submitComment}>
                    <input
                      value={commentText}
                      onChange={(event) => setCommentText(event.target.value)}
                      placeholder="댓글을 남겨보세요"
                      maxLength={500}
                    />
                    <button type="submit" className="ghost-button" disabled={!commentText.trim()}>
                      등록
                    </button>
                  </form>
                  <div className="modal-media-actions">
                    <button
                      type="button"
                      className="ghost-button"
                      onClick={() => openEditMedia(selectedMedia)}
                    >
                      <Pencil size={15} />
                      <span>수정</span>
                    </button>
                    <button
                      type="button"
                      className="text-danger"
                      onClick={() => {
                        const item = selectedMedia
                        setSelectedMedia(null)
                        deleteMedia(item)
                      }}
                    >
                      <Trash2 size={15} />
                      <span>삭제</span>
                    </button>
                  </div>
              </div>
            </div>
          </section>
        </div>
      )}

      {editingMedia && (
        <div className="form-modal-backdrop" onClick={() => setEditingMedia(null)}>
          <form
            className="form-modal stack-form"
            onSubmit={updateMedia}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="section-title">
              <h2>추억 수정</h2>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setEditingMedia(null)}
              >
                <X size={20} />
              </button>
            </div>
            <label>
              연결할 일정
              <select
                value={editForm.eventId}
                onChange={(event) => setEditForm({ ...editForm, eventId: event.target.value })}
              >
                <option value="">일정 없이 등록</option>
                {events.map((event) => (
                  <option key={event.id} value={event.id}>
                    {event.date}
                    {event.endDate && event.endDate !== event.date ? `~${event.endDate}` : ''} · {event.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              제목
              <input
                required
                value={editForm.title}
                onChange={(event) => setEditForm({ ...editForm, title: event.target.value })}
              />
            </label>
            <label>
              촬영일
              <input
                type="date"
                value={editForm.capturedAt}
                onChange={(event) => setEditForm({ ...editForm, capturedAt: event.target.value })}
              />
            </label>
            <label>
              메모
              <textarea
                rows="4"
                value={editForm.memo}
                onChange={(event) => setEditForm({ ...editForm, memo: event.target.value })}
              />
            </label>
            <button type="submit" className="primary-button">
              <Pencil size={17} />
              <span>수정</span>
            </button>
          </form>
        </div>
      )}
    </>
  )
}

export default App
