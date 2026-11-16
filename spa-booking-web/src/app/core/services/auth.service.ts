import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, finalize, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
} from '../models/auth.model';
import { TokenService } from './token.service';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;

  private currentUserSubject!: BehaviorSubject<any>;
  public currentUser$!: Observable<any>;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private router: Router,
  ) {
    this.currentUserSubject = new BehaviorSubject<any>(this.tokenService.getUserInfo());
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  public register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/register`, request)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  public login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/login`, request)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  public refreshToken(refreshToken: string): Observable<AuthResponse> {
    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh-token`, request).pipe(
      tap((response) => {
        this.tokenService.saveTokens(response.accessToken, response.refreshToken);
      }),
    );
  }

  public logout(): void {
    const refreshToken = this.tokenService.getRefreshToken();

    // Nếu không còn refreshToken trong storage thì dọn dẹp local luôn
    if (!refreshToken) {
      this.clearLocalSession();
      return;
    }

    const request: RefreshTokenRequest = { refreshToken };

    // Gọi API thu hồi Access Token (Redis) & Refresh Token (DB)
    // Dùng finalize() để luôn xóa session ở client kể cả khi API trả về 401/lỗi mạng
    this.http
      .post(`${this.API_URL}/logout`, request)
      .pipe(finalize(() => this.clearLocalSession()))
      .subscribe();
  }

  public isLoggedIn(): boolean {
    return !!this.tokenService.getAccessToken();
  }

  private clearLocalSession(): void {
    this.tokenService.clearStorage();
    this.currentUserSubject.next(null);
    this.router.navigate(['/']);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this.tokenService.saveTokens(response.accessToken, response.refreshToken);
    const userInfo = {
      id: response.userId,
      fullName: response.fullName,
      email: response.email,
      roles: response.roles,
    };
    this.tokenService.saveUserInfo(userInfo);
    this.currentUserSubject.next(userInfo);
  }
}
