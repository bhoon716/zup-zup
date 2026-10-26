export const redirectToLogin = () => {
  if (typeof window !== "undefined") {
    if (window.location.pathname !== "/login") {

      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.assign("/login");
    }
  }
};
