import ApiCall from "../../config";
import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { FaTelegramPlane, FaFacebookF, FaYoutube, FaInstagram, FaGlobe } from "react-icons/fa";
import Loading from "./Loading";

function BgImage() {
  const [loading, setLoading] = useState(false);
  const { agentId } = useParams();
  const [open, setOpen] = useState(false);
  const [tel, setTel] = useState("+998");
  const [message, setMessage] = useState("");
  const [isDtm, setIsDtm] = useState(false);
  const [smsCode, setSmsCode] = useState("");
  const [showSmsInput, setShowSmsInput] = useState(false);
  const [timer, setTimer] = useState(60);
  const [isTimerActive, setIsTimerActive] = useState(false);
  const navigate = useNavigate();
  const timerRef = useRef(null);

  const handleClose = () => setOpen(false);

  // Timer effect
  useEffect(() => {
    if (isTimerActive && timer > 0) {
      timerRef.current = setTimeout(() => setTimer(timer - 1), 2000);
    } else if (timer === 0) {
      setIsTimerActive(false);
    }
    return () => clearTimeout(timerRef.current);
  }, [timer, isTimerActive]);

  const handleChange = (e) => {
    let value = e.target.value;
    if (value.length >= 14) return;
    if (value.startsWith("+998") && /^\+998\d{0,9}$/.test(value)) {
      setTel(value);
    } else {
      setTel("+998");
    }
  };

  const handleSmsChange = (e) => {
    const value = e.target.value.replace(/\D/g, ''); // Faqat raqamlarga ruxsat berish
    if (value.length <= 4) {
      setSmsCode(value);
    }
  };

  const startTimer = () => {
    setTimer(120);
    setIsTimerActive(true);
  };

  const resendSms = async () => {
    try {
      setLoading(true);
      // Bu yerda SMS qayta yuborish API chaqiruvi bo'lishi kerak
      // await ApiCall(`/api/v1/resend-sms`, "POST", { phone: tel });
      startTimer();
      setMessage("SMS qayta yuborildi!");
      setOpen(true);
    } catch (error) {
      setMessage("SMS yuborishda xatolik!");
      setOpen(true);
    } finally {
      setLoading(false);
    }
  };

  const getBrowserToken = async () => {
    try {
      const ipRes = await fetch("https://api.ipify.org?format=json");
      const ipData = await ipRes.json();
      const ip = ipData.ip;

      const response = await ApiCall(
        `/api/v1/security/generate`,
        "POST",
        { "Content-Type": "application/x-www-form-urlencoded", "X-Forwarded-For": ip },
        false
      );
      localStorage.setItem("browser_token", response.data);
    } catch (error) {
      console.error("Token olishda xatolik:", error);
    }
  };

  useEffect(() => {
    if (!localStorage.getItem("browser_token")) {
      getBrowserToken();
    }
  }, []);

  const getPhoneData = async (response) => {
    const data = response.data;
    if (!data) {
      navigate("/");
    } else {
      switch (data.status) {
        case 0: navigate("/user-info", { state: { phone: data.phone } }); break;
        case 1: navigate("/data-form", { state: { phone: data.phone } }); break;
        case 2: navigate("/cabinet", { state: { phone: data.phone } }); break;
        case 3:
        case 4: navigate("/test", { state: { phone: data.phone } }); break;
        default: navigate("/");
      }
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);

    const phoneRegex = /^\+998\d{9}$/;
    if (!phoneRegex.test(tel)) {
      setMessage("Telefon raqami noto'g'ri formatda!");
      setOpen(true);
      setLoading(false);
      return;
    }

    // Demo uchun SMS inputini ko'rsatamiz
    setShowSmsInput(true);
    startTimer();
    setLoading(false);
  };

  const verifySmsCode = async () => {
    setLoading(true);

    try {
      const obj = {
        phone: tel,
        agentId: agentId,
        isDtm: isDtm,
        smsCode: smsCode
      };

      const response = await ApiCall(`/api/v1/abuturient`, "POST", obj, null, true);
      await getPhoneData(response);
      setShowSmsInput(false);
    } catch (error) {
      console.error("Error saving data:", error);
      setMessage(error?.response?.data?.message || "Xatolik yuz berdi. SMS kod noto'g'ri!");
      setOpen(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#F6F6F6] min-h-screen">
      {loading && <Loading />}
      <div className="flex pt-10 md:pt-20 justify-center">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h2 className="text-4xl font-bold mt-16 text-[#213972]">XUSH KELIBSIZ!</h2>
            <h2 className="text-xl font-bold mb-8 text-[#213972]">BUXORO XALQARO UNIVERSITETI</h2>
            <div className="bg-white rounded-lg lg:max-w-[960px] max-w-[350px] mx-auto overflow-hidden pb-4">
              <div className="pt-4">
                <h3 className="text-xl font-semibold text-[#213972]">Assalomu alaykum!</h3>
              </div>
              <div className="px-4">
                <h3 className="text-base font-semibold text-[#737373]">
                  Savollaringiz bo'lsa, bog'laning: +998 55 309 99 99<br />
                  Sizga omad tilaymiz!
                </h3>
                <form onSubmit={handleSave} className="space-y-3 md:px-20 mt-4">
                  <p className="text-sm text-[#050929] font-bold mb-1 text-left">Telefon raqami</p>
                  <div className="relative">
                    <input
                      type="text"
                      onChange={handleChange}
                      onClick={() => setTel("+998")}
                      value={tel}
                      className="w-full pl-4 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#D9D9D9] focus:border-[#D9D9D9] outline-none"
                      placeholder="+998 __ ___-__-__"
                      required
                    />
                  </div>

                  {/* SMS Kod Inputi */}
                  {showSmsInput && (
                    <div className="pt-2 animate-fade-in">
                      <p className="text-sm text-[#050929] font-bold mb-1 text-left">SMS kod</p>
                      <div className="relative flex items-center">
                        <input
                          type="text"
                          onChange={handleSmsChange}
                          value={smsCode}
                          className="w-full pl-4 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#D9D9D9] focus:border-[#D9D9D9] outline-none"
                          placeholder="4 xonali kod"
                          maxLength={4}
                        />
                        {isTimerActive && (
                          <span className="absolute right-3 text-sm text-gray-500">
                            {timer}s
                          </span>
                        )}
                      </div>
                      <div className="flex justify-end mt-2">
                        <button
                          type="button"
                          onClick={resendSms}
                          disabled={isTimerActive}
                          className={`text-sm ${isTimerActive ? "text-gray-400" : "text-[#213972] underline"}`}
                        >
                          {isTimerActive ? `Qayta yuborish (${timer}s)` : "Kodni qayta yuborish"}
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="submit"
                      onClick={() => setIsDtm(false)}
                      disabled={showSmsInput && isTimerActive}
                      className={`bg-[#213972] text-white py-2 px-4 rounded-lg transition ${(showSmsInput && isTimerActive) ? "opacity-50 cursor-not-allowed" : "hover:bg-[#1a2c5f]"}`}
                    >
                      Ro'yxatdan o'tish
                    </button>
                    <button
                      type="submit"
                      onClick={() => setIsDtm(true)}
                      disabled={showSmsInput && isTimerActive}
                      className={`bg-[#213972] text-white py-2 px-4 rounded-lg transition ${(showSmsInput && isTimerActive) ? "opacity-50 cursor-not-allowed" : "hover:bg-[#1a2c5f]"}`}
                    >
                      DTM bali bilan talaba bo'lish
                    </button>
                  </div>

                  {/* Tasdiqlash Tugmasi (faqat SMS inputi ko'rinayotganda) */}
                  {showSmsInput && (
                    <div className="flex justify-end pt-2">
                      <button
                        type="button"
                        onClick={verifySmsCode}
                        disabled={smsCode.length !== 6}
                        className={`bg-green-600 text-white py-2 px-4 rounded-lg transition ${smsCode.length !== 6 ? "opacity-50 cursor-not-allowed" : "hover:bg-green-700"}`}
                      >
                        Tasdiqlash
                      </button>
                    </div>
                  )}
                </form>
              </div>
            </div>

            {/* Xabar Modali (agar kerak bo'lsa) */}
            {open && (
              <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white p-6 rounded-lg max-w-sm w-full mx-4">
                  <h3 className="text-xl font-semibold mb-4">{message}</h3>
                  <button
                    onClick={handleClose}
                    className="mt-4 bg-[#213972] text-white py-2 px-4 rounded-lg w-full hover:bg-[#1a2c5f] transition"
                  >
                    OK
                  </button>
                </div>
              </div>
            )}

            <div className="mt-6 pb-4">
              <div className="text-center">
                <h4 className="text-lg text-[#213972]">
                  Murojaat uchun: <br className="md:hidden" />
                  <span className="hidden md:inline"> </span>+998 55 309 99 99
                </h4>
                <div className="flex justify-center gap-4 mt-4">
                  {[
                    { icon: <FaTelegramPlane />, url: "https://t.me/bxu_uz" },
                    { icon: <FaFacebookF />, url: "https://www.facebook.com/BXU.UZ" },
                    { icon: <FaYoutube />, url: "https://www.youtube.com/@bxu_uz" },
                    { icon: <FaInstagram />, url: "https://www.instagram.com/bxu.uz/" },
                    { icon: <FaGlobe />, url: "https://bxu.uz/" }
                  ].map((social, index) => (
                    <a
                      key={index}
                      href={social.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-8 h-8 bg-[#213972] rounded-full flex items-center justify-center text-white hover:bg-blue-800 transition"
                    >
                      {social.icon}
                    </a>
                  ))}
                </div>
              </div>
              <div className="flex items-center justify-center">
                {[1, 2, 3].map((step, index) => (
                  <React.Fragment key={step}>
                    <div className="flex flex-col items-center mt-8">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center 
                      ${step < 2 ? "bg-blue-900 text-white" : "bg-gray-200 text-gray-500"} font-semibold`}>
                        {step}
                      </div>
                      <span className={`text-sm mt-2 ${step < 3 ? "text-blue-900" : "text-gray-400"}`}>
                        {step === 1 ? "Telefon raqam" : step === 2 ? "Ma'lumotnoma" : "Yo'nalish tanlash"}
                      </span>
                    </div>
                    {index < 2 && <div className="flex-1 h-px bg-[#EAEAEC]"></div>}
                  </React.Fragment>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BgImage;