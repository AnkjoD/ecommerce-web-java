import http from "../utils/http";
import { AuthResponse, SuccessResponseApi, AuthUser } from "../types/auth";

const authApi = {
  login: (body: any) => {
    return http.post<AuthUser>("/auth/admin/login", body);
  },
  logout: () => {
    return http.post<SuccessResponseApi<boolean>>("/auth/logout");
  },
  getMe: () => {
    return http.get<AuthUser>("/auth/me");
  }
};

export default authApi;
