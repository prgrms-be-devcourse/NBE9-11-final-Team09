type PasswordVisibilityIconProps = {
  visible: boolean;
};

function PasswordVisibilityIcon({ visible }: PasswordVisibilityIconProps) {
  if (visible) {
    return (
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className="h-5 w-5"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    );
  }

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
    >
      <path d="M3 3l18 18" />
      <path d="M10.6 10.6A3 3 0 0 0 13.4 13.4" />
      <path d="M9.9 4.2A10.8 10.8 0 0 1 12 4c6.5 0 10 8 10 8a18.2 18.2 0 0 1-3.1 4.4" />
      <path d="M6.1 6.1C3.5 8 2 12 2 12s3.5 8 10 8a10.7 10.7 0 0 0 5.9-1.9" />
    </svg>
  );
}

type PasswordFieldProps = {
  label: string;
  placeholder: string;
  value: string;
  visible: boolean;
  disabled?: boolean;
  required?: boolean;
  onChange: (value: string) => void;
  onToggleVisible: () => void;
};

export default function PasswordField({
  label,
  placeholder,
  value,
  visible,
  disabled = false,
  required = false,
  onChange,
  onToggleVisible,
}: PasswordFieldProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-bold">{label}</label>
      <div className="relative">
        <input
          type={visible ? "text" : "password"}
          placeholder={placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={disabled}
          required={required}
          className="w-full rounded border border-gray-300 px-4 py-3 pr-12 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
        />
        <button
          type="button"
          onClick={onToggleVisible}
          disabled={disabled}
          className="absolute inset-y-0 right-0 flex w-12 items-center justify-center text-gray-500 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label={visible ? `${label} 숨기기` : `${label} 보기`}
        >
          <PasswordVisibilityIcon visible={visible} />
        </button>
      </div>
    </div>
  );
}
