import api from "@/shared/api/client";
import type { WishlistResponse, WishlistToggleResponse } from '@/shared/types/api';

export const getMyWishlist = async (): Promise<WishlistResponse[]> => {
  const { data } = await api.get('/api/v1/wishlist');
  return data;
};

export const toggleWishlist = async (courseKey: string): Promise<WishlistToggleResponse> => {
  const { data } = await api.post(`/api/v1/wishlist/${encodeURIComponent(courseKey)}`);
  return data;
};
