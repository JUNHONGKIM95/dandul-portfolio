self.addEventListener('push', (event) => {
  const fallback = {
    title: 'DANDUL',
    body: '새 알림이 도착했어요.',
    url: '/',
    tag: 'dandul-notification',
    icon: '/dandul-favicon.svg',
    badge: '/dandul-favicon.svg',
  }

  const data = event.data ? event.data.json() : fallback
  const title = data.title || fallback.title
  const options = {
    body: data.body || fallback.body,
    tag: data.tag || fallback.tag,
    icon: data.icon || fallback.icon,
    badge: data.badge || fallback.badge,
    data: {
      url: data.url || fallback.url,
    },
  }

  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const targetUrl = new URL(event.notification.data?.url || '/', self.location.origin).href

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      const existingClient = clients.find((client) => client.url === targetUrl)
      if (existingClient) {
        return existingClient.focus()
      }
      return self.clients.openWindow(targetUrl)
    }),
  )
})
