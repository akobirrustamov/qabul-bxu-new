import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Header from "../header/Header";
import ApiCall, { baseUrl } from "../../config";
import Select from "react-select";
import { FaTelegramPlane, FaFacebookF, FaYoutube, FaInstagram, FaGlobe } from "react-icons/fa";
import Loading from "./Loading";

function DataForm() {
  const [isLoading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const phone = location.state?.phone || "";
  const [appealType, setAppealType] = useState([]);
  const [educationType, setEducationType] = useState([]);
  const [educationForm, setEducationForm] = useState([]);
  const [educationField, setEducationField] = useState([]);
  const [abuturient, setAbuturient] = useState({
    phone: phone,
    language: true,
    appealTypeId: "",
    educationTypeId: "",
    educationFormId: "",
    educationFieldId: "",
    createdAt: new Date().toISOString(),
  });

  useEffect(() => {
    getAppealType();
    getEducationType()
  }, []);

  const getPhoneData = async (response) => {
    // try {
    //   const response = await ApiCall(
    //     `/api/v1/history-of-abuturient/${phone}`,
    //     "POST",
    //     null,
    //     null,
    //     true
    //   );
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

  const getAppealType = async () => {
    try {
      const response = await ApiCall(
        `/api/v1/appeal-type`,
        "GET",
        null,
        null,
        true
      );
      setAppealType(
        response.data.map((type) => ({
          value: type.id,
          label: type.name,
        }))
      );
    } catch (error) {
      console.error("Error fetching appeal types:", error);
    }
  };

  const getEducationType = async () => {
    try {
      const response = await ApiCall(
        `/api/v1/education-type`,
        "GET",
        null,
        null,
        true
      );
      setEducationType(
        response.data.map((type) => ({
          value: type.id,
          label: type.name,
        }))
      );
    } catch (error) {
      console.error("Error fetching education types:", error);
    }
  };
  const getEducationField = async (id) => {
    try {
      const response = await ApiCall(
        `/api/v1/education-field/${id}`,
        "GET",
        null,
        null,
        true
      );
      setEducationField(
        response.data
          .filter((field) => field.isActive === true) // Filter only active fields
          .map((field) => ({
            value: field.id,
            label: field.name,
            status: field.isActive, // Include status in the option object
          }))
      );
    } catch (error) {
      console.error("Error fetching education fields:", error);
    }
  };

  const getEducationForm = async (id) => {
    try {
      const response = await ApiCall(
        `/api/v1/education-form/active/${id}`,
        "GET",
        null,
        null,
        true
      );
      setEducationForm(
        response.data.map((form) => ({
          value: form.id,
          label: form.name,
        }))
      );
    } catch (error) {
      console.error("Error fetching education forms:", error);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    if (
      !abuturient.appealTypeId ||
      !abuturient.educationTypeId ||
      !abuturient.educationFormId ||
      !abuturient.educationFieldId
    ) {
      alert("Iltimos, barcha maydonlarni to'ldiring!");
      return;
    }

    try {
      // Barcha ma'lumotlarni birlashtirish
      const response = await ApiCall(
        `/api/v1/abuturient/data-form`,
        "PUT",
        abuturient,
        null,
        true
      );
      setLoading(false);
      getPhoneData(response);
    } catch (error) {
      alert(error.response?.data?.message || "Xatolik yuz berdi. Ma'lumotni saqlashning iloji bo'lmadi.");
    } finally {
      setLoading(false);
    }

    navigate("/cabinet", { state: { phone: phone } });
  }
  const handleSelectChange = (selectedOption, { name }) => {
    setAbuturient({ ...abuturient, [name]: selectedOption.value });

    if (name === "educationTypeId") {
      getEducationForm(selectedOption.value);
      setAbuturient((prev) => ({
        ...prev,
        educationFormId: "",
        educationFieldId: "",
      }));
      setEducationForm([]);
      setEducationField([]);
    } else if (name === "educationFormId") {
      getEducationField(selectedOption.value);
      setAbuturient((prev) => ({
        ...prev,
        educationFieldId: "",
      }));
      setEducationField([]);
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
        {isLoading && <Loading />}
        <div className="container mx-auto px-4 pt-32 flex flex-col h-full">
          <div className="">
            <div className="bg-white p-6 md:p-8 rounded-lg shadow-lg w-full max-w-4xl mx-auto">
              <h2 className="text-xl md:text-lg font-bold mb-3 text-center text-[#213972]">
                Ro'yxatdan o'tish
              </h2>
              <p className="text-[#737373] text-base text-center">
                Ro'yxatdan o'tish uchun ma'lumotni to'ldiring!
              </p>
              <form onSubmit={handleSave} className="space-y-6">
                <div className="grid gap-6">
                  {/* Appeal Type */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Ariza turi
                    </label>
                    <Select
                      name="appealTypeId"
                      value={appealType.find(
                        (option) =>
                          option.value === abuturient.appealTypeId
                      )}
                      onChange={handleSelectChange}
                      options={appealType}
                      placeholder="Ariza turini tanlang"
                      isSearchable
                      styles={customStyles}
                      required
                    />
                  </div>
                  {/* Education Type */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Ta'lim turi
                    </label>
                    <Select
                      name="educationTypeId"
                      value={educationType.find(
                        (option) =>
                          option.value ===
                          abuturient.educationTypeId
                      )}
                      onChange={handleSelectChange}
                      options={educationType}
                      placeholder="Ta'lim turini tanlang"
                      isSearchable
                      styles={customStyles}
                      required
                    />
                  </div>
                  {/* Education Form */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Ta'lim shakli
                    </label>
                    <Select
                      name="educationFormId"
                      value={educationForm.find(
                        (option) =>
                          option.value ===
                          abuturient.educationFormId
                      )}
                      onChange={handleSelectChange}
                      options={educationForm}
                      placeholder={
                        abuturient.educationTypeId
                          ? "Ta'lim shaklini tanlang"
                          : "Avval ta'lim turini tanlang"
                      }
                      isDisabled={!abuturient.educationTypeId}
                      isSearchable
                      styles={customStyles}
                      required
                    />
                  </div>
                  {/* Education Field */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1 z-auto">
                      Yo'nalish
                    </label>
                    <Select
                      name="educationFieldId"
                      value={educationField.find(
                        (option) =>
                          option.value ===
                          abuturient.educationFieldId
                      )}
                      onChange={handleSelectChange}
                      options={educationField}
                      placeholder={
                        abuturient.educationFormId
                          ? "Yo'nalishni tanlang"
                          : "Avval ta'lim shaklini tanlang"
                      }
                      isDisabled={!abuturient.educationFormId}
                      isSearchable
                      styles={customStyles}
                      menuPortalTarget={document.body}
                      menuPosition="fixed"
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
            <div className="flex items-center justify-center mt-10">
              <div className="flex flex-col items-center">
                <div className="md:py-3 md:px-4 py-2 px-3 rounded-full flex items-center justify-center bg-[#213972] text-white font-semibold">
                  1
                </div>
                <span className="text-sm text-[#213972] mt-2 textl-xl">Telefon raqam</span>
              </div>
              <div className="flex-1 h-px bg-[#EAEAEC] mb-4"></div>  {/* mb-4 -> mt-4 */}
              <div className="flex flex-col items-center">
                <div className="md:py-3 md:px-4 py-2 px-3 rounded-full flex items-center justify-center bg-[#213972] text-white font-semibold">
                  2
                </div>
                <span className="text-sm text-[#213972] mt-2 textl-xl">Ma'lumotnoma</span>
              </div>
              <div className="flex-1 h-px bg-[#EAEAEC] mb-4"></div>  {/* mb-4 -> mt-4 */}
              <div className="flex flex-col items-center">
                <div className="md:py-3 md:px-4 py-2 px-3 rounded-full flex items-center justify-center bg-[#213972] text-white font-semibold">
                  3
                </div>
                <span className="text-sm text-[#213972] mt-2 textl-xl">Yo'nalish tanlash</span>
              </div>
            </div>
          </div>
        </div>
      </section >
    </div>
  );
};

export default DataForm;