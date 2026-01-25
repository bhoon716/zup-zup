import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as userApi from "@/lib/api/user";
import type { UserDeviceRequest } from "@/types/api";
import { toast } from "sonner";

export const useRegisterDevice = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UserDeviceRequest) => userApi.registerDevice(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-profile"] });
      toast.success("기기가 등록되었습니다.");
    },
    onError: () => {
      toast.error("기기 등록에 실패했습니다.");
    },
  });
};

export const useUnregisterDevice = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (token: string) => userApi.unregisterDevice(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-profile"] });
      toast.success("기기 등록이 해제되었습니다.");
    },
    onError: () => {
      toast.error("기기 해제에 실패했습니다.");
    },
  });
};
