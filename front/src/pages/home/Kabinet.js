import React, { use } from 'react'
import Header from '../header/Header'
import logo from "../../images/logoMain.png"
import ApiCall from '../../config/index'
import { useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { FaTelegramPlane, FaFacebookF, FaYoutube, FaInstagram, FaGlobe } from "react-icons/fa";
import Loading from './Loading';


function Kabinet() {
  const [loading, setLoading] = useState(false);
  const [isChecked, setIsChecked] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const [score, setScore] = useState("");
  const [isScoreValid, setIsScoreValid] = useState(true);
  const phone = location.state?.phone || "";

  const [abuturient, setAbuturient] = useState({
    firstName: "",
    lastName: "",
    fatherName: "",
    phone: phone || "",
    regionId: "",
    districtId: "",
    passportNumber: "",
    passportPin: "",
    language: true,
    appealTypeId: "",
    educationTypeId: "",
    educationFormId: "",
    educationFieldId: "",
    isDtm: false,
    createdAt: new Date().toISOString(),
  });

  const handleScoreChange = (e) => {
    const value = e.target.value;
    if (value === '' || /^[0-9\b]+$/.test(value)) {
      if (value === '' || parseInt(value) <= 189) {
        setScore(value);
        setIsScoreValid(true);
      }
    }
  };

  const handleSubmit = async () => {
    await ApiCall(`/api/v1/abuturient/isdtm/` + abuturient.id, "POST", { isdtm: isChecked });

    if (isChecked) {
      // Agar DTM talaba bo'lsa
      if (!score || score === "") {
        setIsScoreValid(false);
        return;
      }

      // Ballni serverga yuborish
      try {
        setLoading(true);
        const resultData = {
          score: parseInt(score),
          showScore: parseInt(score),
        };

        await ApiCall(`/api/v1/test/result/${phone}`, "POST", resultData, null, true);
        navigate("/test", { state: { phone: phone } });
        setLoading(false);
        getPhoneData();
      } catch (error) {
        console.error("Error submitting test data:", error);
        setLoading(false);
      }
    } else {
      // Agar DTM talaba bo'lmasa
      navigate("/test", { state: { phone: phone } });
    }
  };

  useEffect(() => {
    fetchAbuturientData();
    getPhoneData();
    localStorage.removeItem("browser_token")
  }, []);

  const getPhoneData = async () => {
    try {
      const response = await ApiCall(
        `/api/v1/history-of-abuturient/${phone}`,
        "POST",
        null,
        null,
        true
      );
    } catch (error) {
      console.error("Error fetching data:", error);
    }

    if (!phone || phone === "" || phone === null || phone === undefined) {
      navigate("/");
    } else {
      try {
        const response = await ApiCall(
          `/api/v1/abuturient/${phone}`,
          "GET",
          null,
          null,
          true
        );

        if (response.data === null || response.data === undefined) {
          navigate("/");
        } else if (response.data.status == 0) {
          navigate("/user-info", { state: { phone: phone } });
        } else if (response.data.status == 1) {
          navigate("/data-form", { state: { phone: phone } });
        } else if (response.data.status == 2) {
          navigate("/cabinet", { state: { phone: phone } });
        } else if (response.data.status == 3 || response.data.status == 4) {
          navigate("/test", { state: { phone: phone } })
        } else {
          navigate("/");
        }
      } catch (error) {
        console.error("Error fetching data:", error);
      }
    }
  };

  const fetchAbuturientData = async () => {
    try {
      const response = await ApiCall(`/api/v1/abuturient/${phone}`, "GET", null, null, true);
      if (response.data) {
        setAbuturient(response.data);
      } else {
        console.error("No data found");
      }
    } catch (error) {
      console.error("Error fetching abuturient data:", error);
    }
  };


  return (
    <div>
      <Header />
      <div className='bg-[#F6F6F6] lg:pt-32 pt-16 pb-10'>
        <div className='bg-[#ffffff] lg:w-[960px] w-[356px] mx-auto rounded-lg shadow-lg py-6 lg:px-20 px-4 mt-10'>
          <div className='flex flex-col items-center'>
            <img src={logo} className='md:w-28 md:h-28 w-20 h-20' />
            <h2 className='font-semibold text-base text-[#213972] mt-2 mb-1 lg:text-xl'>
              Tasdiqlash
            </h2>
            <p className='text-[#CE1126] text-xs text-center md:text-base mb-8'>
              Eslatma: Kiritilgan ma'lumotlar tog'ri ekanligini tekshiring!
            </p>
          </div>
          <div>
            <div className='bg-[#B8DEFF] text-xs w-full py-2 pl-2 lg:py-3 lg:pl-3 text-[#454545] font-semibold lg:text-base'>
              Shaxsiy ma'lumotlari
            </div>
            <div>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Ism
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient.firstName}
              </p>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Familiya
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient.lastName}
              </p>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Sharif
              </p>
              {abuturient.fatherName ? (
                <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                  {abuturient.fatherName}
                </p>
              ) :
                (
                  <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                    Otasining ismi kiritilmagan
                  </p>
                )
              }
              <div>
                <div className='block lg:flex gap-4 w-full'>
                  <div className='w-full lg:w-1/2'>
                    <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                      Passport seriya raqami
                    </p>
                    <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                      {abuturient.passportNumber}
                    </p>
                  </div>
                  <div className='w-full lg:w-1/2'>
                    <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                      JSHSHIR
                    </p>
                    <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                      {abuturient.passportPin}
                    </p>
                  </div>
                </div>
                <div className='block lg:flex gap-4 w-full'>
                  <div className='w-full lg:w-1/2'>
                    <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                      Viloyat
                    </p>
                    <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                      {abuturient?.district?.region?.name || "Viloyat ma'lumotlari mavjud emas"}
                    </p>
                  </div>
                  <div className='w-full lg:w-1/2'>
                    <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                      Shahar/Tuman
                    </p>
                    <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                      {abuturient.district?.name || "Shahar/Tuman ma'lumotlari mavjud emas"}
                    </p>
                  </div>
                </div>
              </div>

              <div className='bg-[#B8DEFF] text-xs w-full py-2 pl-2 lg:py-3 lg:pl-3 text-[#454545] font-semibold lg:text-base'>Ta'lim yo'nalish ma'lumotlari</div>
              <p className='text-xs lg:text-base font-semibold text-[#454545] mt-2'>
                Ariza turi
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient?.appealType?.name || "Ariza turi ma'lumotlari mavjud emas"}
              </p>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Ta'lim turi
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient?.educationField?.educationForm?.educationType?.name || "Ta'lim turi ma'lumotlari mavjud emas"}
              </p>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Ta'lim shakli
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient?.educationField?.educationForm?.name || "Ta'lim shakli ma'lumotlari mavjud emas"}
              </p>
              <p className='text-xs lg:text-base font-semibold text-[#454545] my-2 m-0'>
                Ta'lim yo'nalishi
              </p>
              <p className='py-3 pl-3 w-full border border-[#D9D9D9] rounded-md text-sm font-medium lg:text-base'>
                {abuturient?.educationField?.name || "Ta'lim yo'nalishi ma'lumotlari mavjud emas"}
              </p>
              <div className='mb-4'>
                <div className='bg-[#B8DEFF] text-xs w-full py-2 pl-2 lg:py-3 lg:pl-3 text-[#454545] font-semibold lg:text-base flex items-center'>
                  <input
                    type="checkbox"
                    className='mr-2 h-4 w-4 lg:h-5 lg:w-5'
                    checked={isChecked}
                    onChange={(e) => {
                      setIsChecked(e.target.checked);
                      if (!e.target.checked) {
                        setIsScoreValid(true);
                      }
                    }}
                  />
                  DTM ballni kiriting {!abuturient?.isDtm && "(Majburiy emas)"}
                </div>

                {isChecked && (
                  <div className='mt-2'>
                    <input
                      type="text"
                      value={score}
                      onChange={handleScoreChange}
                      className={`w-full p-2 border ${!abuturient?.isDtm || isScoreValid ? 'border-gray-300' : 'border-red-500'} rounded focus:outline-none focus:ring-2 focus:ring-blue-500`}
                      placeholder={abuturient?.isDtm ? '0-189 (Majburiy)' : '0-189 (Majburiy emas)'}
                      inputMode='numeric'
                      pattern='[0-9]*'
                    />
                    {abuturient?.isDtm && !isScoreValid && (
                      <p className='text-xs text-red-500 mt-1'>Iltimos, DTM ballini kiriting (0-189)</p>
                    )}

                  </div>
                )}
              </div>
              <div className="flex justify-end">
                <button
                  type="button"
                  className={`bg-[#213972] text-white py-2 px-4 rounded-lg transition duration-300 ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
                  onClick={handleSubmit}
                  disabled={loading}
                >
                  {loading ? 'Yuborilmoqda...' : 'Davom etish'}
                </button>
              </div>

            </div>
          </div>
        </div>
        <h4 className="hidden md:block text-center text-lg md:text-xl text-[#213972] mt-14">Murojaat uchun: +998 55 309 99 99</h4>
        <div className="flex items-center justify-center gap-4 mt-3">
          <a href="https://t.me/bxu_uz" target="_blank" rel="noopener noreferrer" className="w-6 h-6 md:w-8 md:h-8 bg-[#213972] rounded-full flex items-center justify-center text-white text-sm md:text-base">
            <FaTelegramPlane />
          </a>
          <a href="https://www.facebook.com/BXU.UZ" target="_blank" rel="noopener noreferrer" className="w-6 h-6 md:w-8 md:h-8 bg-[#213972] rounded-full flex items-center justify-center text-white text-sm md:text-base">
            <FaFacebookF />
          </a>
          <a href="https://www.youtube.com/@bxu_uz" target="_blank" rel="noopener noreferrer" className="w-6 h-6 md:w-8 md:h-8 bg-[#213972] rounded-full flex items-center justify-center text-white text-sm md:text-base">
            <FaYoutube />
          </a>
          <a href="https://www.instagram.com/bxu.uz/" target="_blank" rel="noopener noreferrer" className="w-6 h-6 md:w-8 md:h-8 bg-[#213972] rounded-full flex items-center justify-center text-white text-sm md:text-base">
            <FaInstagram />
          </a>
          <a href="https://bxu.uz/" target="_blank" rel="noopener noreferrer" className="w-6 h-6 md:w-8 md:h-8 bg-[#213972] rounded-full flex items-center justify-center text-white text-sm md:text-base">
            <FaGlobe />
          </a>
        </div>
      </div>
    </div>
  )
}

export default Kabinet;