import api from './index';
import type { CommonResponse, WishlistResponse, WishlistToggleResponse } from '@/types/api';

export const getMyWishlist = async (): Promise<CommonResponse<WishlistResponse[]>> => {
  const { data } = await api.get('/api/v1/wishlist');
  return data;
};

export const toggleWishlist = async (courseKey: string): Promise<CommonResponse<WishlistToggleResponse>> => {
  const { data } = await api.post(`/api/v1/wishlist/${courseKey}`);
  return data;
};
