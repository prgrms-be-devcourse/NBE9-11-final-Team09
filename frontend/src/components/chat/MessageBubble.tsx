import { ChatMessageResponse } from "@/types/chat";
import { parseBackendKstDateTime } from "@/lib/dateTime";

interface MessageBubbleProps {
    message: ChatMessageResponse;
}

export default function MessageBubble({ message }: MessageBubbleProps) {
    const { host, content, createdAt } = message;
    const isHost = host;

    // 시간 포맷 (예: "오후 4:02")
    const formattedTime = parseBackendKstDateTime(createdAt).toLocaleTimeString("ko-KR", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
        timeZone: "Asia/Seoul",
    });

    // 말풍선 색상/스타일
    const bubbleClass = isHost
        ? "bg-white border border-[#dbe7dc] text-slate-800 rounded-tl-none"
        : "bg-[#4f7a61] text-white rounded-tr-none";

    return (
        <div className={`flex ${isHost ? "justify-start" : "justify-end"} mb-3`}>
            <div className="flex flex-col max-w-[70%]">
                {isHost && (
                    <span className="mb-1 ml-1 text-xs text-slate-500">주최자</span>
                )}
                <div className={`px-4 py-2 rounded-2xl text-sm ${bubbleClass}`}>
                    {content}
                </div>
                <span
                    className={`mt-1 text-xs text-slate-400 ${
                        isHost ? "text-left ml-1" : "text-right mr-1"
                    }`}
                >
          {formattedTime}
        </span>

            </div>
        </div>
    );
}
