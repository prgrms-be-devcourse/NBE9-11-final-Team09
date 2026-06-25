const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "";

export function createApiUrl(path: string) {
  if (path.startsWith("http")) {
    return path;
  }

  return `${API_BASE_URL}${path}`;
}