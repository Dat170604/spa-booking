import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, RefreshTokenRequest, RegisterRequest } from '../models/auth.model';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;

  private currentUserSubject!: BehaviorSubject<any>;
  public currentUser$!: Observable<any>;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService
  ) {
    this.currentUserSubject = new BehaviorSubject<any>(this.tokenService.getUserInfo());
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  public register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  public login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  public refreshToken(refreshToken: string): Observable<AuthResponse> {
    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh-token`, request).pipe(
      tap((response) => {
        this.tokenService.saveTokens(response.accessToken, response.refreshToken);
      })
    );
  }

  public logout(): void {
    this.tokenService.clearStorage();
    this.currentUserSubject.next(null);
  }

  public isLoggedIn(): boolean {
    return !!this.tokenService.getAccessToken();
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this.tokenService.saveTokens(response.accessToken, response.refreshToken);
    const userInfo = {
      id: response.userId,
      fullName: response.fullName,
      email: response.email,
      roles: response.roles
    };
    this.tokenService.saveUserInfo(userInfo);
    this.currentUserSubject.next(userInfo);
  }

}
