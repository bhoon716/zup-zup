import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as wishlistApi from '@/lib/api/wishlist';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { WishlistResponse } from '@/types/api';

export const useWishlist = () => {
  return useQuery({
    queryKey: ['wishlist'],
    queryFn: async () => {
      const response = await wishlistApi.getMyWishlist();
      return response.data ?? null;
    },
  });
};

export const useToggleWishlist = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (courseKey: string) => wishlistApi.toggleWishlist(courseKey),
    onMutate: async (courseKey) => {
      await queryClient.cancelQueries({ queryKey: ['wishlist'] });
      
      const previousWishlist = queryClient.getQueryData<WishlistResponse[]>(['wishlist']);
      
      // Optimistically update: If exists, remove it. If not, add a temporary item (partial).
      // Since we don't have full course details for 'add', optimistic update might be tricky for the list view.
      // But for the heart icon state in the table, we usually derive it from the list data.
      // So, adding a placeholder record is important.
      
      if (previousWishlist) {
         const exists = previousWishlist.some(item => item.courseKey === courseKey);
         let newWishlist;
         
         if (exists) {
            newWishlist = previousWishlist.filter(item => item.courseKey !== courseKey);
         } else {
             // Mocking new item. Since we don't display detailed info in the heart icon logic,
             // just having an item with the courseKey is sufficient for the "isWished" check.
             newWishlist = [...previousWishlist, { 
                id: -1, // temporary ID
                courseKey, 
                courseName: '', 
                professor: '', 
                // ...other empty fields 
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
