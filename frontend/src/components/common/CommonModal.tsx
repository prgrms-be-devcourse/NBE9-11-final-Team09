// CommonModal 컴포넌트가 받는 props 타입
interface CommonModalProps {
    title: string;        // 모달 제목
    message: string;      // 모달 메시지
    onClose: () => void;  // 닫기 버튼 클릭 핸들러
}

// 공통 모달 컴포넌트
export default function CommonModal({
                                        title,
                                        message,
                                        onClose,
                                    }: CommonModalProps) {
    return (
        <div
            className="absolute inset-0 bg-black/30 flex items-center justify-center z-50 rounded-xl"
            onClick={onClose} // 배경 클릭 시 닫기
        >
            {/* 모달 박스 */}
            <div
                className="bg-white rounded-xl p-6 w-80 shadow-lg"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 제목 */}
                <h3 className="text-base font-semibold mb-2">{title}</h3>

                {/* 메시지 */}
                <p className="text-sm text-gray-600 mb-6">{message}</p>

                {/* 확인 버튼 */}
                <button
                    className="w-full py-2 bg-black text-white rounded-lg text-sm font-medium hover:bg-gray-800"
                    onClick={onClose}
                >
                    확인
                </button>
            </div>
        </div>
    );
}