export const redirectToLogin = () => {
  if (typeof window !== "undefined") {
    if (window.location.pathname !== "/login") {
      window.location.assign("/login");
    }
  }
};
