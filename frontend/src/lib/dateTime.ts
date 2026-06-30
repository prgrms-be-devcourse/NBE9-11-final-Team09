const TIME_ZONE_SUFFIX_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i;

export function parseBackendUtcDateTime(value: string) {
  return new Date(TIME_ZONE_SUFFIX_PATTERN.test(value) ? value : `${value}Z`);
}
