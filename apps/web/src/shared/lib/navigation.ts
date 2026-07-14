export const redirectToLogin = () => {
  if (typeof window !== "undefined") {
    if (window.location.pathname !== "/login") {
      // This helper runs from the Axios interceptor, outside React's routing context.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.assign("/login");
    }
  }
};
