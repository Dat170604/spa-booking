import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';
import { TokenService } from './token.service';
import { AppNotification } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService implements OnDestroy {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);

  private apiUrl = `${environment.apiUrl}/notifications`;
  private stompClient: Client | null = null;

  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  /**
   * Khởi tạo dữ liệu và mở kết nối WebSocket khi người dùng đã đăng nhập
   */
  public initRealtimeNotifications(): void {
    const token = this.tokenService.getAccessToken();
    if (!token) return;

    this.loadInitialData();
    this.connectWebSocket(token);
  }

  private loadInitialData(): void {
    this.http.get<AppNotification[]>(this.apiUrl).subscribe({
      next: (list) => {
        this.notificationsSubject.next(list);
        this.unreadCountSubject.next(list.filter((n) => !n.read).length);
      },
    });
  }

  private connectWebSocket(token: string): void {
    if (this.stompClient?.active) return;

    // Tạo URL gốc cho WebSocket (ví dụ http://localhost:8080/ws)
    const wsUrl = environment.apiUrl.replace('/api/v1', '') + '/ws';

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.stompClient?.subscribe('/user/queue/notifications', (message: IMessage) => {
          const newNotif: AppNotification = JSON.parse(message.body);
          const currentList = this.notificationsSubject.value;
          this.notificationsSubject.next([newNotif, ...currentList]);
          this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
        });
      },
    });

    this.stompClient.activate();
  }

  public markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, null);
  }

  public markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, null);
  }

  public localMarkAsRead(id: number): void {
    const updated = this.notificationsSubject.value.map((n) =>
      n.id === id ? { ...n, read: true } : n,
    );
    this.notificationsSubject.next(updated);
    this.unreadCountSubject.next(updated.filter((n) => !n.read).length);
  }

  public localMarkAllAsRead(): void {
    const updated = this.notificationsSubject.value.map((n) => ({ ...n, read: true }));
    this.notificationsSubject.next(updated);
    this.unreadCountSubject.next(0);
  }

  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
