interface ImportMetaEnv {
  readonly VITE_API_BASE?: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_AUTH_STRATEGY?: "cookie" | "bearer";
  readonly VITE_STORE_TOKEN?: "local" | "memory";
  readonly VITE_PROFILE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}