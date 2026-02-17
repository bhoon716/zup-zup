import { describe, it, expect } from 'vitest';
import api from "./client";

describe('API Interceptors', () => {
  it('registers response interceptor on axios instance', () => {
    expect(api.interceptors).toBeDefined();
    expect(api.interceptors.response).toBeDefined();
    expect(typeof api.interceptors.response.use).toBe('function');
  });
});
