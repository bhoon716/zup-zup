import api from "@/shared/api/client";
import type { CommonResponse, WishlistResponse, WishlistToggleResponse } from '@/shared/types/api';

export const getMyWishlist = async (): Promise<CommonResponse<WishlistResponse[]>> => {
  const { data } = await api.get('/api/v1/wishlist');
  return data;
};

export const toggleWishlist = async (courseKey: string): Promise<CommonResponse<WishlistToggleResponse>> => {
  const { data } = await api.post(`/api/v1/wishlist/${courseKey}`);
  return data;
};
