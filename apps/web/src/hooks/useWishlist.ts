import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as wishlistApi from '@/lib/api/wishlist';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { WishlistResponse } from '@/types/api';

import { useUser } from './useUser';

export const useWishlist = () => {
  const { data: user } = useUser();
  
  return useQuery({
    queryKey: ['wishlist'],
    queryFn: async () => {
      const response = await wishlistApi.getMyWishlist();
      return response.data ?? null;
    },
    enabled: !!user,
  });
};

export const useToggleWishlist = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (courseKey: string) => wishlistApi.toggleWishlist(courseKey),
    onMutate: async (courseKey) => {
      await queryClient.cancelQueries({ queryKey: ['wishlist'] });
      
      const previousWishlist = queryClient.getQueryData<WishlistResponse[]>(['wishlist']);

      if (previousWishlist) {
         const exists = previousWishlist.some(item => item.courseKey === courseKey);
         let newWishlist;
         
         if (exists) {
            newWishlist = previousWishlist.filter(item => item.courseKey !== courseKey);
         } else {
             // 하트 토글 즉시 반영을 위해 최소 필드만 가진 임시 레코드를 넣는다.
             newWishlist = [...previousWishlist, { 
                id: -1,
                courseKey, 
                courseName: '', 
                professor: '', 
             } as WishlistResponse];
         }
         
         queryClient.setQueryData(['wishlist'], newWishlist);
      }
      
      return { previousWishlist };
    },
    onError: (err, newTodo, context) => {
      queryClient.setQueryData(['wishlist'], context?.previousWishlist);
      const message = (err as AxiosError<{ message: string }>).response?.data?.message || '찜 상태 변경에 실패했습니다';
      toast.error(message);
    },
    onSuccess: (response) => {
       toast.success(response.message || '찜 목록이 업데이트되었습니다.');
       queryClient.invalidateQueries({ queryKey: ['wishlist'] });
    },
  });
};
