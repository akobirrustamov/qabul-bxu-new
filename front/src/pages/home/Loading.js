import React from "react";
import { FaSpinner } from "react-icons/fa";

function Loading() {
    return (
        <div className="fixed inset-0 bg-[#F6F6F6] bg-opacity-90 flex items-center justify-center z-50">
            <div className="text-center">
                <div className="flex justify-center">
                    <FaSpinner className="animate-spin text-[#213972] text-4xl" />
                </div>
                <h2 className="text-xl sm:text-2xl font-bold mt-4 text-[#213972]">
                    BUXORO XALQARO UNIVERSITETI
                </h2>
                <p className="text-[#213972] mt-2">Yuklanmoqda...</p>
            </div>
        </div>
    );
}

export default Loading;