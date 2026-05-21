import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

let refreshing = false;
const refreshSubject$ = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const withBearer = (r: HttpRequest<unknown>, token: string) =>
    r.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  const token = auth.getAccessToken();
  const authReq = token ? withBearer(req, token) : req;

  return next(authReq).pipe(
    catchError(err => {
      if (err.status !== 401 || req.url.includes('/api/auth/refresh')) {
        return throwError(() => err);
      }

      const rt = auth.getRefreshToken();
      if (!rt) {
        auth.logout();
        return throwError(() => err);
      }

      if (refreshing) {
        return refreshSubject$.pipe(
          filter(t => t !== null),
          take(1),
          switchMap(newToken => next(withBearer(req, newToken!)))
        );
      }

      refreshing = true;
      refreshSubject$.next(null);

      return auth.refresh(rt).pipe(
        switchMap(resp => {
          refreshing = false;
          refreshSubject$.next(resp.accessToken);
          return next(withBearer(req, resp.accessToken));
        }),
        catchError(e => {
          refreshing = false;
          auth.logout();
          return throwError(() => e);
        })
      );
    })
  );
};
