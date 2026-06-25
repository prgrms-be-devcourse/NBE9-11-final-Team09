export interface Notification {
    notificationId: number;
    fundingId: number;
    notificationType: string;
    title: string;
    content: string;
    emailSentAt: string;
  }
  
  export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
  }