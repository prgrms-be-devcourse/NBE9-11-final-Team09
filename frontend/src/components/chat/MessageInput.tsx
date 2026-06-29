import { useState } from "react";

interface MessageInputProps {
    onSend: (content: string) => void;
    disabled?: boolean;
}

export default function MessageInput({ onSend, disabled }: MessageInputProps) {
    const [input, setInput] = useState("");

    const handleSend = () => {
        const trimmedInput = input.trim();

        if (!trimmedInput) return;

        onSend(trimmedInput);
        setInput("");
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.nativeEvent.isComposing) return;
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="flex items-center gap-2 border-t border-[#dbe7dc] bg-white p-4">
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={disabled}
                placeholder="주최자에게 문의할 내용을 입력하세요..."
                className="flex-1 rounded-full border border-[#dbe7dc] px-4 py-2 text-sm outline-none transition focus:border-[#4f7a61] focus:ring-3 focus:ring-[#4f7a61]/10 disabled:bg-slate-100"
            />
            <button
                type="button"
                onClick={handleSend}
                disabled={disabled || !input.trim()}
                className="rounded-full bg-[#4f7a61] px-4 py-2 text-sm font-semibold text-white hover:bg-[#426f55] disabled:opacity-50"
            >
                전송
            </button>
        </div>
    );
}
