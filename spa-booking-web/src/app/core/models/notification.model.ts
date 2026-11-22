export interface AppNotification {
  id: number;
  recipientEmail: string;
  title: string;
  message: string;
  type: 'BOOKING_CREATED' | 'BOOKING_STATUS' | 'LEAVE_REQUEST' | 'LEAVE_STATUS';
  targetUrl: string | null;
  read: boolean;
  createdAt: string;
}
