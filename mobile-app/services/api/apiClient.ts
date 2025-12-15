import axios, { AxiosInstance, AxiosError } from 'axios';
import { ErrorResponse } from './types';

// Configure base URL - use your local IP for device testing
// In development, replace with your machine's local IP address
const getBaseUrl = (): string => {
  if (__DEV__) {
    // For iOS Simulator, localhost works
    // For Android Emulator, use 10.0.2.2
    // For physical devices, use your computer's local IP (e.g., 192.168.1.x)
    return 'http://localhost:8080/api';
  }
  // Production URL - replace with your actual API URL
  return 'https://api.your-domain.com/api';
};

const API_TIMEOUT = 30000; // 30 seconds

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: getBaseUrl(),
      timeout: API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for logging
    this.client.interceptors.request.use(
      (config) => {
        if (__DEV__) {
          console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
        }
        return config;
      },
      (error) => {
        console.error('[API] Request error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => {
        if (__DEV__) {
          console.log(`[API] Response ${response.status}:`, response.data);
        }
        return response;
      },
      (error: AxiosError<ErrorResponse>) => {
        if (error.response) {
          const errorData = error.response.data;
          console.error(`[API] Error ${error.response.status}:`, errorData);

          // Throw a more descriptive error
          const message = errorData?.message || error.message;
          const apiError = new Error(message) as Error & { code?: string; status?: number };
          apiError.code = errorData?.code;
          apiError.status = error.response.status;
          return Promise.reject(apiError);
        }

        if (error.request) {
          console.error('[API] No response received:', error.message);
          return Promise.reject(new Error('Network error - no response received'));
        }

        console.error('[API] Request setup error:', error.message);
        return Promise.reject(error);
      }
    );
  }

  async get<T>(url: string): Promise<T> {
    const response = await this.client.get<T>(url);
    return response.data;
  }

  async post<T, D = unknown>(url: string, data?: D): Promise<T> {
    const response = await this.client.post<T>(url, data);
    return response.data;
  }

  // Allow updating base URL at runtime (useful for development)
  setBaseUrl(url: string): void {
    this.client.defaults.baseURL = url;
  }
}

export const apiClient = new ApiClient();
