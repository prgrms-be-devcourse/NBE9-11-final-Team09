const TIME_ZONE_SUFFIX_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i;

export function parseBackendKstDateTime(value: string) {
  const normalized = value.trim().replace(" ", "T");

  if (!normalized.includes("T")) {
    return new Date(`${normalized}T00:00:00+09:00`);
  }

  return new Date(
    TIME_ZONE_SUFFIX_PATTERN.test(normalized)
      ? normalized
      : `${normalized}+09:00`
  );
}
