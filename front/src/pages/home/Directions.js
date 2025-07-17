import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Header from "../header/Header";
import ApiCall, { baseUrl } from "../../config";
import Select from "react-select";
import passfront from "./passfront.png";
import id from "./id.png";
import { FaTelegramPlane, FaFacebookF, FaYoutube, FaInstagram, FaGlobe } from "react-icons/fa";
import Loading from "./Loading";


function Directions() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const phone = location.state?.phone || "";
  // console.log(phone);
  const [regions, setRegions] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [loadingDistricts, setLoadingDistricts] = useState(false);

  const [abuturient, setAbuturient] = useState({
    firstName: "",
    lastName: "",
    fatherName: "",
    phone: phone || "",
    regionId: "",
    districtId: "",
    passportNumber: "",
    passportPin: "",
  });
  const getPhoneData = async (response) => {
    // try {
    //   const response = await ApiCall(
    //     `/api/v1/history-of-abuturient/${phone}`,
    //     "POST",
    //     null,
    //     null,
    //     true
    //   );
    //   console.log(response);

    // } catch (error) {
    //   console.error("Error fetching data:", error);
    // }
    if (!phone || phone === "" || phone === null || phone === undefined) {
      navigate("/");
    } else
      try {


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
  };
  useEffect(() => {
    fetchRegions();
  }, []);

  const fetchRegions = async () => {
    try {
      const response = await ApiCall("/api/v1/region", "GET", null, null);
      setRegions(
        response.data.map((region) => ({
          value: region.id,
          label: region.name,
        }))
      );
    } catch (error) {
      console.error("Error fetching regions:", error);
    }
  };

  const fetchRegionDistricts = async (regionId) => {
    setLoadingDistricts(true);
    try {
      const response = await ApiCall(
        `/api/v1/district/${regionId}`,
        "GET",
        null,
        null
      );
      setDistricts(
        response.data.map((district) => ({
          value: district.id,
          label: district.name,
        }))
      );
    } catch (error) {
      console.error("Error fetching districts:", error);
    } finally {
      setLoadingDistricts(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    if (
      !abuturient.firstName ||
      !abuturient.lastName ||
      !abuturient.phone ||
      !abuturient.regionId ||
      !abuturient.districtId ||
      !abuturient.passportNumber ||
      abuturient.passportNumber.length !== 9 ||
      !abuturient.passportPin ||
      abuturient.passportPin.length !== 14
    ) {
      alert("Iltimos, barcha maydonlarni to'ldiring!");
      return;
    }

    try {
      const response = await ApiCall(
        `/api/v1/abuturient/user-info`,
        "PUT",
        abuturient,
        null,
        true
      );
      getPhoneData(response);
      // navigate("/data-form", { state: phone });
    } catch (error) {
      alert(error.response?.data?.message || "Xatolik yuz berdi. Ma'lumotni saqlashning iloji bo'lmadi.");
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    if (name === "passportPin") {
      const numericValue = value.replace(/\D/g, "");
      if (numericValue.length <= 14) {
        setAbuturient((prev) => ({ ...prev, [name]: numericValue }));
      }
      return;
    }
    if (name === "passportNumber") {
      const formattedValue = value.toUpperCase();
      const letters = formattedValue.slice(0, 2).replace(/[^A-Z]/g, "");
      const numbers = formattedValue.slice(2).replace(/\D/g, "");
      const passportNumber = `${letters}${numbers.slice(0, 7)}`;
      setAbuturient((prev) => ({ ...prev, [name]: passportNumber }));
      return;
    }
    setAbuturient({ ...abuturient, [name]: value });
  };

  const handleSelectChange = (selectedOption, { name }) => {
    setAbuturient({ ...abuturient, [name]: selectedOption.value });
    if (name === "regionId") {
      fetchRegionDistricts(selectedOption.value);
      setAbuturient((prev) => ({
        ...prev,
        districtId: "",
      }));
      setDistricts([]);
    }
  };

  const customStyles = {
    control: (provided) => ({
      ...provided,
      minHeight: "40px",
      borderRadius: "0.375rem",
      borderColor: "#d1d5db",
      "&:hover": {
        borderColor: "#3b82f6",
      },
      boxShadow: "none",
    }),
    option: (provided, state) => ({
      ...provided,
      backgroundColor: state.isSelected ? "#3b82f6" : "white",
      color: state.isSelected ? "white" : "#1f2937",
      "&:hover": {
        backgroundColor: "#e5e7eb",
      },
    }),
    menu: (provided) => ({
      ...provided,
      zIndex: 9999,
      position: "absolute",
      marginTop: "0.25rem",
    }),
    menuPortal: (provided) => ({
      ...provided,
      zIndex: 9999,
    }),
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden">
      <div>
        <Header />
      </div>
      <section className="bg-[#F6F6F6] flex-1 overflow-y-auto">
        {loading && <Loading />}
        <div className="container mx-auto px-4 pt-32 flex flex-col h-full">
          <div className="">
            <div className="bg-white p-6 md:p-8 rounded-lg shadow-lg w-full max-w-4xl mx-auto">
              <h2 className="text-xl md:text-lg font-bold mb-3 text-center text-[#213972]">
                Ro'yxatdan o'tish
              </h2>
              <p className="text-[#737373] text-base text-center">
                Ro'yxatdan o'tish uchun ma'lumotni to'ldiring!
              </p>
              <form onSubmit={handleSave} className="space-y-2 mt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-left">
                  {/* Last Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Familiya
                    </label>
                    <input
                      type="text"
                      name="lastName"
                      value={abuturient.lastName}
                      onChange={handleInputChange}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>
                  {/* First Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Ism
                    </label>
                    <input
                      type="text"
                      name="firstName"
                      value={abuturient.firstName}
                      onChange={handleInputChange}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>

                  {/* Father's Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Sharifi
                    </label>
                    <input
                      type="text"
                      name="fatherName"
                      value={abuturient.fatherName}
                      onChange={handleInputChange}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>

                  {/* Phone */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Telefon raqami { }
                    </label>
                    <input
                      type="text"
                      name="phone"
                      disabled={true}
                      value={abuturient.phone}
                      onChange={handleInputChange}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-gray-100"
                      required
                    />
                  </div>

                  {/* Passport Number */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Passport seriya raqami
                    </label>
                    <input
                      type="text"
                      name="passportNumber"
                      value={abuturient.passportNumber}
                      onChange={handleInputChange}
                      className="border border-gray-300 rounded-md p-1 w-full"
                      placeholder="AA1234567"
                    />

                  </div>
                  {/* JSHIR */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      JSHSHIR
                    </label>
                    <input
                      type="text"
                      name="passportPin"
                      value={abuturient.passportPin}
                      onChange={handleInputChange}
                      className="border border-gray-300 rounded-md p-1 w-full"
                      placeholder="12345678901234"
                    />

                  </div>

                  {/* Region */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Viloyat
                    </label>
                    <Select
                      name="regionId"
                      value={regions.find(
                        (option) =>
                          option.value === abuturient.regionId
                      )}
                      onChange={handleSelectChange}
                      options={regions}
                      placeholder="Viloyatni tanlang"
                      isSearchable
                      styles={customStyles}
                      required
                    />
                  </div>

                  {/* District */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Tuman/Shahar
                    </label>
                    <Select
                      name="districtId"
                      value={districts.find(
                        (option) =>
                          option.value === abuturient.districtId
                      )}
                      onChange={handleSelectChange}
                      options={districts}
                      placeholder={
                        loadingDistricts
                          ? "Yuklanmoqda..."
                          : "Tuman/Shaharni tanlang"
                      }
                      isDisabled={
                        !abuturient.regionId || loadingDistricts
                      }
                      isSearchable
                      styles={customStyles}
                      required
                    />
                  </div>
                </div>

                {/* Save Button */}
                <div className="flex justify-end">
                  <button
                    type="submit"
                    className="bg-[#213972] text-white py-2 px-4 rounded-lg transition duration-300"
                  >
                    Davom etish
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* Progress Steps - Now at the bottom */}
          <div className="mt-12">
            {/* Contact Info */}
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
                      ${step < 3 ? "bg-blue-900 text-white" : "bg-gray-200 text-gray-500"} font-semibold`}>
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
      </section >
    </div >
  );
}

export default Directions;