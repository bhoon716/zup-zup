export const redirectToLogin = () => {
  if (typeof window !== "undefined") {
    window.location.assign("/login");
  }
};
