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
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="flex items-center gap-2 p-4 border-t border-gray-200 bg-white">
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={disabled}
                placeholder="주최자에게 문의할 내용을 입력하세요..."
                className="flex-1 px-4 py-2 border border-gray-300 rounded-full text-sm outline-none focus:border-gray-500 disabled:bg-gray-100"
            />
            <button
                type="button"
                onClick={handleSend}
                disabled={disabled || !input.trim()}
                className="px-4 py-2 bg-gray-800 text-white text-sm rounded-full disabled:opacity-50"
            >
                전송
            </button>
        </div>
    );
}