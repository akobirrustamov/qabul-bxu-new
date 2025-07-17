import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import ApiCall, { baseUrl } from "../../config";
import Header from "../header/Header";
import Confetti from "react-confetti";
import Zoom from "react-reveal/Zoom";
import { FaDownload } from "react-icons/fa";
import logo from "../../images/logoMain.png";
import check from "./check.png"
import {
    FaInstagram,
    FaTelegram,
    FaYoutube,
    FaPhoneAlt,
    FaMapMarkerAlt,
} from "react-icons/fa";

import Loading from "./Loading";

function TestAbiturient() {
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const [showTest, setShowTest] = useState(true);
    const location = useLocation();
    const [answers, setAnswers] = useState({});
    const [correctCount, setCorrectCount] = useState(null);
    const [showConfetti, setShowConfetti] = useState(false);
    const [subjectNames, setSubjectNames] = useState([]);
    const [totalScore, setTotalScore] = useState(null);
    const [formData, setFormData] = useState(null);
    const [failed, setFailed] = useState(false)
    const [certificate1, setCertificate1] = useState(null)
    const [certificate2, setCertificate2] = useState(null)
    const [certificate3, setCertificate3] = useState(null)
    const [ijodiy, setIjodiy] = useState(false)
    const [subjects, setSubjects] = useState({
        subject1: [],
        subject2: [],
        subject3: [],
        subject4: [],
        subject5: [],
    });
    const [canSubmit, setCanSubmit] = useState(false);
    const phone = location.state?.phone || "";
    // const phone = "+998900829474";


    useEffect(() => {
        if (!phone) {
            navigate("/");
        }

        // Save the phone number in local storage
        const savedPhone = localStorage.getItem("phone");
        if (savedPhone !== phone) {
            // If the phone number has changed, clear old test data
            localStorage.removeItem("testData");
            localStorage.removeItem("answers");
        }

        // Save the new phone number in local storage
        localStorage.setItem("phone", phone);

        // Fetch data for the new phone number
        getPhoneData();
        // fetchTestData();
    }, []);

    useEffect(() => {

    }, [correctCount]);

    useEffect(() => {
        checkIfAllQuestionsAnswered();
    }, [answers]);

    const getPhoneData = async () => {
        try {
            const response = await ApiCall(`/api/v1/abuturient/${phone}`, "GET", null, null, true);
            if (response.data === null) {
                navigate("/");
            } else if (response.data) {
                setFormData(response.data);
                if (response.data.appealType.id == 2) {
                    setShowTest(false);
                    setIjodiy(true);
                }
                if (response.data.status >= 3) {

                    setShowTest(false);
                    getScore();
                } else {

                    const savedTest = localStorage.getItem("testData");
                    if (savedTest) {

                        setSubjects(JSON.parse(savedTest));
                    } else {
                        fetchTestData();
                    }
                }
            }
        } catch (error) {
            console.error("Error fetching data:", error);
        }
    };

    const getScore = async () => {
        try {
            const response = await ApiCall(`/api/v1/test/score/${phone}`, "GET", null, null, true);
            if (response.data === null) {
                navigate("/");
            } else if (response.data) {
                setTotalScore(response.data?.score);
                if (response.data?.score < 57) {
                    setFailed(true)
                }
            }
        } catch (error) {
            console.error("Error fetching data:", error);
        }
    };

    const fetchTestData = async () => {
        try {
            const response = await ApiCall(`/api/v1/test/${phone}`, "GET", null, null, true);
            if (response === null || response === undefined || response.data === null || response.data === undefined) {
                showTest(false);
                setIjodiy(true);
            }
            if (response.data) {
                if (response.data.subject5.length === 0 || response.data.subject5.length === 0) {
                    setShowTest(false);
                    setIjodiy(true)
                    return;
                }
                const subjectsData = {
                    subject1: response.data.subject1 || [],
                    subject2: response.data.subject2 || [],
                    subject3: response.data.subject3 || [],
                    subject4: response.data.subject4 || [],
                    subject5: response.data.subject5 || [],
                };

                setSubjectNames(response?.data?.subjects);
                setSubjects(subjectsData);
                localStorage.setItem("testData", JSON.stringify(subjectsData));
                setFailed(false)
                setShowTest(true)
                // Check if subject4 is empty

            } else {

                navigate("/");
            }
        } catch (error) {
            setShowTest(false);
            setIjodiy(true);
            console.error("Error fetching test data:", error);
        }
    };

    const handleAnswerChange = (questionId, optionIndex) => {
        setAnswers((prev) => {
            const updatedAnswers = { ...prev, [questionId]: optionIndex };
            localStorage.setItem("answers", JSON.stringify(updatedAnswers));
            return updatedAnswers;
        });
    };

    useEffect(() => {
        const savedAnswers = localStorage.getItem("answers");
        if (savedAnswers) {
            setAnswers(JSON.parse(savedAnswers));
        }
    }, []);

    const checkIfAllQuestionsAnswered = () => {
        const allQuestions = Object.values(subjects)
            .flat()
            .map((q) => q.id);


        let ans = localStorage.getItem("answers")
        if (ans) {
            const allAnswered = allQuestions.every((id) => Object.keys(answers).includes(id.toString()));
            setCanSubmit(allAnswered);
        }
    };

    const handleSubmit = async () => {
        const calculateScore = (subject, weight) => {
            return subject.reduce((score, question) => {
                const selectedOption = answers[question.id];
                return selectedOption !== undefined && question.answer == selectedOption
                    ? score + weight
                    : score;
            }, 0);
        };
        // alert(JSON.stringify(subjects.subject1))

        const score1 = calculateScore(subjects.subject1, 2.2);
        const score2 = calculateScore(subjects.subject2, 2.2);
        const score3 = calculateScore(subjects.subject3, 2.2);
        const score4 = calculateScore(subjects.subject4, 6.3);
        const score5 = calculateScore(subjects.subject5, 9.3);
        // alert(score1)
        // alert(score2)
        // alert(score3)
        // alert(score4)

        const total = Number((score1 + score2 + score3 + score5 + score4).toFixed(1));

        // alert(total)

        setShowTest(false);
        let finalScore = total;
        if (!ijodiy) {
            if (total < 57) {
                setFailed(true)

            } else {
                setShowConfetti(true);
                const timer = setTimeout(() => setShowConfetti(false), 7000);
                // return () => clearTimeout(timer);

            }
        }


        setCorrectCount(finalScore);
        setTotalScore(finalScore);

        const resultData = {
            score: parseFloat((total || 0).toFixed(1)),
            showScore: parseFloat((finalScore || 0).toFixed(1)),
        };


        try {
            await ApiCall(`/api/v1/test/result/${phone}`, "POST", resultData, null, true);
            localStorage.clear();
            getPhoneData()
        } catch (error) {
            console.error("Error submitting test data:", error);
        }
    };

    const handleDownloadPDF = async () => {
        setLoading(true); // Yuklash boshlanishida loading ni true qilamiz
        try {
            const response = await fetch(`${baseUrl}/api/v1/abuturient/contract/${phone}`, {
                method: 'GET',
            });

            if (!response.ok) {
                throw new Error("Failed to download file");
            }

            const contentType = response.headers.get('Content-Type');
            if (!contentType || !contentType.includes('application/pdf')) {
                throw new Error("The response is not a valid PDF file.");
            }

            const blob = await response.blob();
            if (!blob.size) {
                throw new Error("The PDF file is empty.");
            }

            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.download = `Contract_${phone}.pdf`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(downloadUrl);
            console.log("PDF downloaded successfully");
        } catch (error) {
            console.error("Error downloading PDF:", error);
        } finally {
            setLoading(false); // Yuklash tugaganda loading ni false qilamiz
        }
    };
    const [shuffledAnswers, setShuffledAnswers] = useState({});
    useEffect(() => {
        if (Object.keys(subjects).length > 0) {
            const shuffled = {};
            Object.keys(subjects).forEach((subjectKey) => {
                subjects[subjectKey].forEach((question) => {
                    const options = [question.answer, question.wrongAnswer1, question.wrongAnswer2, question.wrongAnswer3];
                    shuffled[question.id] = options.sort(() => Math.random() - 0.5); // Shuffle once
                });
            });
            setShuffledAnswers(shuffled);
        }
    }, [subjects]);

    const handleMainImageChange = (e) => {
        setCertificate1(e.target.files[0]);
    };
    return (
        <div>
            <Header />
            <div>
                {showConfetti && <Confetti />}

                {showTest ? (
                    <div className={"pt-24 lg:pt-32 bg-[#F6F6F6]"}>
                        {/* <div className={"border-2 mt-0 m-4 my-0 rounded bg-cyan-300 p-2 "}>
                            <h1 className="text-xl font-semibold">
                                {formData?.lastName} {formData?.firstName} {formData?.fatherName}
                            </h1>
                            <p className="text-xl">
                                Ta'lim turi: {formData?.educationField?.educationForm?.educationType?.name}
                            </p>
                            <p className="text-xl">
                                Ta'lim shakli: {formData?.educationField?.educationForm?.name}
                            </p>
                            <p className="text-xl m-2">
                                Ta'lim yonalishi: {formData?.educationField?.name}
                            </p>
                        </div> */}
                        <div className={""}>
                            <div className="lg:flex gap-4 p-4 pt-0 w-full mx-auto lg:p-4">
                                <div className="col-span-1 md:col-span-2 pt-2 w-full lg:w-4/6">
                                    <div className="test-container ">
                                        {
                                            Object.keys(subjects).reduce((acc, subjectKey) => {
                                                if (!subjects[subjectKey]?.length) return acc;

                                                return [
                                                    ...acc,
                                                    ...subjects[subjectKey].map((question, idx) => {
                                                        const globalIndex = acc.length + idx + 1;
                                                        return (
                                                            <div key={question.id} id={`question-${question.id}`} className="rounded-xl p-3 mb-4 bg-white">
                                                                <span className="font-semibold text-[#737373] text-xs lg:text-lg">
                                                                    {globalIndex}-savol /
                                                                </span>
                                                                <span className="font-semibold text-[#737373] text-xs lg:text-lg">
                                                                    {question.testSubject.name?.includes("_0") ? "1-blok" : "2-blok"} / {question.testSubject.description}
                                                                </span>
                                                                <p className="text-base text-[#000000] lg:text-xl font-medium">
                                                                    {question.question}
                                                                </p>
                                                                {shuffledAnswers[question.id]?.map((option, i) => (
                                                                    <label
                                                                        key={i}
                                                                        className={`flex items-center gap-1 text-sm lg:text-base rounde ${answers[question.id] === option
                                                                            ? "border-[#256DF6] text-[#256DF6]"
                                                                            : "border-[#737373] text-[#737373]"
                                                                            }`}
                                                                    >
                                                                        <input
                                                                            id={`question-${question.id}-${i}`}
                                                                            type="radio"
                                                                            name={`question-${question.id}`}
                                                                            value={option}
                                                                            checked={answers[question.id] === option}
                                                                            onChange={() => handleAnswerChange(question.id, option)}
                                                                            className="accent-[#256DF6]"
                                                                        />
                                                                        {option}
                                                                    </label>
                                                                ))}
                                                            </div>

                                                        );
                                                    }),
                                                ];
                                            }, [])
                                        }
                                    </div>
                                </div>
                                <div className="col-span-1 md:col-span-2 w-full lg:w-2/6"
                                    style={{ position: "sticky", top: "0", maxHeight: "100vh", overflowY: "auto" }}
                                >
                                    <div>
                                        <h4 className="p-2 pt-0 font-semibold">Savollar</h4>

                                        <div className="space-y-4 p-2 bg-white">
                                            {Object.values(subjects)
                                                .flat()
                                                .reduce((acc, question, index) => {
                                                    const blocLabel = question.testSubject.name?.includes("_0")
                                                        ? "1-blok / " + question.testSubject.description
                                                        : "2-blok / " + question.testSubject.description;

                                                    const lastGroup = acc[acc.length - 1];

                                                    if (!lastGroup || lastGroup.label !== blocLabel || lastGroup.questions.length >= 5) {
                                                        acc.push({ label: blocLabel, questions: [{ ...question, index: index + 1 }] });
                                                    } else {
                                                        lastGroup.questions.push({ ...question, index: index + 1 });
                                                    }
                                                    return acc;
                                                }, [])
                                                .map((group, groupIndex) => (
                                                    <div key={groupIndex} className="border-2 rounded-xl p-3 border-[#256DF6] bg-[#004CFF08]">
                                                        <p className="text-sm font-medium mb-2">{group.label}</p>
                                                        <div className="grid grid-cols-5 gap-2 p-0">
                                                            {group.questions.map((q) => (
                                                                <div
                                                                    key={q.id}
                                                                    className={`${answers[q.id] ? "bg-blue-600 text-white" : "bg-white border text-[#737373]"}
                  text-center p-2 rounded-xl cursor-pointer`}
                                                                    onClick={() => {
                                                                        const el = document.getElementById(`question-${q.id}`);
                                                                        if (el) el.scrollIntoView({ behavior: "smooth" });
                                                                    }}
                                                                >
                                                                    <span className="text-base font-bold">{q.index}</span>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    </div>
                                                ))}
                                        </div>

                                        <button
                                            onClick={handleSubmit}
                                            className={`block mx-auto px-4 py-2 mb-4 rounded mt-4 font-semibold ${canSubmit
                                                ? "bg-blue-500 text-white"
                                                : "bg-gray-300 text-gray-500 cursor-not-allowed"
                                                }`}
                                            disabled={!canSubmit}
                                        >
                                            Testni yakunlash
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                ) : (
                    (failed && formData.ball < 56) ?
                        <div className={"pt-20 lg:pt-32 bg-[#F6F6F6] text-center"}>
                            <Zoom>
                                <div className={"px-4 m-4 mt-0 bg-cover  "}
                                >
                                    <h3 className={"text-[#213972] xl:text-3xl md:text-xl sm:text-sm"}>Siz testdan yetarli balni to'play olmadingiz. </h3>
                                    <h3 className={"text-[#213972] xl:text-3xl md:text-xl sm:text-sm"}> Sizning to'plagan balingiz: {formData.ball} </h3>
                                    <div>
                                        <button
                                            onClick={() => {
                                                localStorage.clear();
                                                setAnswers({});
                                                setSubjects({
                                                    subject1: [],
                                                    subject2: [],
                                                    subject3: [],
                                                    subject4: [],
                                                    subject5: [],
                                                })

                                                fetchTestData(); // Funksiyani chaqirish
                                            }}
                                            className="bg-[#213972] p-2 rounded my-2 text-white hover:shadow-2xl transform"
                                        >
                                            Testni qayta topshirish
                                        </button>

                                    </div>
                                    <div className='flex-1 min-w-[250px]  '>
                                        <div className="">
                                            <div className="">
                                                {/* Map Section */}
                                                <div className="flex flex-col items-center">
                                                    <div className="flex items-center mb-4">
                                                        <h2 className="text-2xl md:text-3xl font-bold text-[#213972]">
                                                            Buxoro Xalqaro Universiteti manzili
                                                        </h2>
                                                    </div>

                                                    <div className="w-[950px] overflow-hidden rounded-xl border-2 border-[#213972]">
                                                        <iframe
                                                            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3064.8958577959097!2d64.42846967583635!3d39.80932777154381!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3f5009003f1c477b%3A0x920d498788a13e58!2sBuxoro%20psixologiya%20va%20xorijiy%20tillar%20instituti!5e0!3m2!1sru!2s!4v1728054121217!5m2!1sru!2s"
                                                            allowFullScreen
                                                            loading="lazy"
                                                            className="w-[950px] h-64 md:h-96 border-2 border-[#213972] rounded-xl"
                                                            referrerPolicy="no-referrer-when-downgrade"
                                                        ></iframe>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                {/* Footer */}
                                <div className="bg-[#213972] rounded-2xl p-6 xl:p-8 shadow-lg mx-20">
                                    <div className="flex flex-col md:flex-row justify-between items-center space-y-6 md:space-y-0 ">
                                        {/* Logo */}
                                        <div className="flex-shrink-0">
                                            <a
                                                href="/"
                                                className="flex items-center gap-2 hover:scale-105 transition duration-300"
                                            >
                                                <img
                                                    src={logo}
                                                    alt="Logo"
                                                    className="lg:w-[80px] lg:h-[80px] w-[70px] h-[70px]"
                                                />
                                                <span className="text-white lg:text-2xl text-base no-underline text-left">
                                                    Buxoro Xalqaro <br /> Universiteti
                                                </span>
                                            </a>
                                        </div>

                                        {/* Phone Number */}
                                        <div className="flex items-center bg-white/10 backdrop-blur-sm rounded-full px-6 py-3">
                                            <FaPhoneAlt className="text-white mr-3 text-lg" />
                                            <a
                                                href="tel:+998553099999"
                                                className="text-white hover:text-gray-200 text-lg md:text-xl font-semibold transition-colors"
                                            >
                                                +998 (55) 309-99-99
                                            </a>
                                        </div>

                                        {/* Social Icons */}
                                        <div className="flex space-x-4">
                                            <a
                                                href="https://www.instagram.com/bxu.uz?igsh=bHQ3YmRvajR1aXYy"
                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                aria-label="Instagram"
                                            >
                                                <FaInstagram className="text-white text-2xl" />
                                            </a>
                                            <a
                                                href="https://t.me/bxu_uz"
                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                aria-label="Telegram"
                                            >
                                                <FaTelegram className="text-white text-2xl" />
                                            </a>
                                            <a
                                                href=""
                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                aria-label="YouTube"
                                            >
                                                <FaYoutube className="text-white text-2xl" />
                                            </a>
                                        </div>
                                    </div>


                                </div>
                            </Zoom>
                        </div>
                        :
                        <div className={""}>
                            <Zoom>
                                <div className={""}>
                                    {ijodiy ? (
                                        <div className={"pt-4 bg-[#F6F6F6] text-center"}>

                                            <div className={"pt-20 lg:pt-32 bg-[#F6F6F6] text-center mx-auto max-w-4xl"}>
                                                <h3 className={"font-semibold text-4xl text-[#213972] lg:text-5xl"}>Ijodiy imtihonni topshirish uchun siz institutga markaziy binosiga tashrif buyurishingiz so'raladi.</h3>
                                                <h3 className={"font-semibold text-[#213972] text-base lg:text-2xl mb-4"}>Manzil: Buxoro shahri Sitorayi Mohi-Xosa MFY G'ijduvon ko'chasi 250-uy</h3>
                                                {formData.ball > 56 && (
                                                    <div className="mb-2 bg-white mx-4 py-4 lg:py-10  lg:p-5 rounded-xl">
                                                        <img src={check} alt="Check" className="w-24 h-24 mx-auto mb-4" />
                                                        <h3 className={"font-semibold text-xl text-[#213972] lg:text-3xl"}> Sizning to'plagan balingiz: {formData.ball}</h3>
                                                        <button
                                                            onClick={handleDownloadPDF}
                                                            className="w-full flex items-center mt-8 justify-between gap-2 text-[#256DF6] font-semibold border-2 border-[#256DF6] rounded-md p-2 bg-[#004CFF0D]"
                                                            disabled={loading}
                                                        >
                                                            {loading ? (
                                                                <Loading /> // Yoki o'zingizning Loading komponentingiz
                                                            ) : (
                                                                <>
                                                                    <span className="text-sm lg:text-xl">Shartnoma yuklab olish</span>
                                                                    <span className="text-base lg:text-2xl"><FaDownload /></span>
                                                                </>
                                                            )}
                                                        </button>
                                                    </div>
                                                )}
                                                <h3 className="text-2xl font-medium text-[#154476] lg:text-4xl my-4">Buxoro Xalqaro Universiteti manzili</h3>
                                                <div className="mx-auto w-[320px] lg:w-[950px]  overflow-hidden rounded-xl border-2 border-[#213972]">
                                                    <iframe
                                                        src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3064.8958577959097!2d64.42846967583635!3d39.80932777154381!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3f5009003f1c477b%3A0x920d498788a13e58!2sBuxoro%20psixologiya%20va%20xorijiy%20tillar%20instituti!5e0!3m2!1sru!2s!4v1728054121217!5m2!1sru!2s"
                                                        allowFullScreen
                                                        loading="lazy"
                                                        className="w-[950px] h-64 md:h-96 border-2 border-[#213972] rounded-xl"
                                                        referrerPolicy="no-referrer-when-downgrade"
                                                    ></iframe>
                                                </div>
                                                {/* Footer */}
                                                <div className="bg-[#213972] rounded-2xl p-6 xl:p-8 shadow-lg mx-4 mt-6 lg:mx-20">
                                                    <div className="flex flex-col md:flex-row justify-between items-center space-y-6 md:space-y-0 ">
                                                        {/* Logo */}
                                                        <div className="flex-shrink-0">
                                                            <a
                                                                href="/"
                                                                className="flex items-center gap-2 hover:scale-105 transition duration-300"
                                                            >
                                                                <img
                                                                    src={logo}
                                                                    alt="Logo"
                                                                    className="lg:w-[80px] lg:h-[80px] w-[70px] h-[70px]"
                                                                />
                                                                <span className="text-white lg:text-2xl text-base no-underline text-left">
                                                                    Buxoro Xalqaro <br /> Universiteti
                                                                </span>
                                                            </a>
                                                        </div>

                                                        {/* Phone Number */}
                                                        <div className="flex items-center bg-white/10 backdrop-blur-sm rounded-full px-6 py-3">
                                                            <FaPhoneAlt className="text-white mr-3 text-lg" />
                                                            <a
                                                                href="tel:+998553099999"
                                                                className="text-white hover:text-gray-200 text-lg md:text-xl font-semibold transition-colors"
                                                            >
                                                                +998 (55) 309-99-99
                                                            </a>
                                                        </div>

                                                        {/* Social Icons */}
                                                        <div className="flex space-x-4">
                                                            <a
                                                                href="https://www.instagram.com/bxu.uz?igsh=bHQ3YmRvajR1aXYy"
                                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                                aria-label="Instagram"
                                                            >
                                                                <FaInstagram className="text-white text-2xl" />
                                                            </a>
                                                            <a
                                                                href="https://t.me/bxu_uz"
                                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                                aria-label="Telegram"
                                                            >
                                                                <FaTelegram className="text-white text-2xl" />
                                                            </a>
                                                            <a
                                                                href=""
                                                                className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                                aria-label="YouTube"
                                                            >
                                                                <FaYoutube className="text-white text-2xl" />
                                                            </a>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className={"pt-4 bg-[#F6F6F6] text-center"}>
                                            <div className={"pt-20 lg:pt-32 bg-[#F6F6F6] text-center mx-auto max-w-4xl"}>
                                                <h3 className={"font-semibold text-4xl text-[#213972] lg:text-5xl"}>Tabriklaymiz</h3>
                                                <h3 className={"font-semibold text-[#213972] text-base lg:text-2xl mb-4"}>Tabriklaymiz siz talabalar safiga qabul
                                                    qilindingiz!<br />
                                                    Sizni talabalar saimizda
                                                    ko'rishimizdan xursand bo'lamiz.</h3>
                                                <div className="mb-2 bg-white mx-4 py-4 lg:py-10  lg:p-5 rounded-xl">
                                                    <img src={check} alt="Check" className="w-24 h-24 mx-auto mb-4" />
                                                    <h3 className={"font-semibold text-xl text-[#213972] lg:text-3xl"}> Sizning to'plagan balingiz: {formData.ball}</h3>
                                                    <div className="px-4">
                                                        <button
                                                            onClick={handleDownloadPDF}
                                                            className="w-full flex items-center mt-8 justify-between gap-2 text-[#256DF6] font-semibold border-2 border-[#256DF6] rounded-md p-2 bg-[#004CFF0D]"
                                                            disabled={loading}
                                                        >
                                                            {loading ? (
                                                                <Loading /> // Yoki o'zingizning Loading komponentingiz
                                                            ) : (
                                                                <>
                                                                    <span className="text-sm lg:text-xl">Shartnoma yuklab olish</span>
                                                                    <span className="text-base lg:text-2xl"><FaDownload /></span>
                                                                </>
                                                            )}
                                                        </button>
                                                    </div>
                                                </div>
                                                <h3 className="text-2xl font-medium text-[#154476] lg:text-4xl my-4">Buxoro Xalqaro Universiteti manzili</h3>
                                                <div className="mx-auto w-[320px] lg:w-[950px]  overflow-hidden rounded-xl border-2 border-[#213972]">
                                                    <iframe
                                                        src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3064.8958577959097!2d64.42846967583635!3d39.80932777154381!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3f5009003f1c477b%3A0x920d498788a13e58!2sBuxoro%20psixologiya%20va%20xorijiy%20tillar%20instituti!5e0!3m2!1sru!2s!4v1728054121217!5m2!1sru!2s"
                                                        allowFullScreen
                                                        loading="lazy"
                                                        className="w-[950px] h-64 md:h-96 border-2 border-[#213972] rounded-xl"
                                                        referrerPolicy="no-referrer-when-downgrade"
                                                    ></iframe>
                                                </div>
                                            </div>
                                            {/* Footer */}
                                            <div className="bg-[#213972] rounded-2xl p-6 xl:p-8 shadow-lg mx-4 mt-6 lg:mx-20">
                                                <div className="flex flex-col md:flex-row justify-between items-center space-y-6 md:space-y-0 ">
                                                    {/* Logo */}
                                                    <div className="flex-shrink-0">
                                                        <a
                                                            href="/"
                                                            className="flex items-center gap-2 hover:scale-105 transition duration-300"
                                                        >
                                                            <img
                                                                src={logo}
                                                                alt="Logo"
                                                                className="lg:w-[80px] lg:h-[80px] w-[70px] h-[70px]"
                                                            />
                                                            <span className="text-white lg:text-2xl text-base no-underline text-left">
                                                                Buxoro Xalqaro <br /> Universiteti
                                                            </span>
                                                        </a>
                                                    </div>

                                                    {/* Phone Number */}
                                                    <div className="flex items-center bg-white/10 backdrop-blur-sm rounded-full px-6 py-3">
                                                        <FaPhoneAlt className="text-white mr-3 text-lg" />
                                                        <a
                                                            href="tel:+998553099999"
                                                            className="text-white hover:text-gray-200 text-lg md:text-xl font-semibold transition-colors"
                                                        >
                                                            +998 (55) 309-99-99
                                                        </a>
                                                    </div>

                                                    {/* Social Icons */}
                                                    <div className="flex space-x-4">
                                                        <a
                                                            href="https://www.instagram.com/bxu.uz?igsh=bHQ3YmRvajR1aXYy"
                                                            className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                            aria-label="Instagram"
                                                        >
                                                            <FaInstagram className="text-white text-2xl" />
                                                        </a>
                                                        <a
                                                            href="https://t.me/bxu_uz"
                                                            className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                            aria-label="Telegram"
                                                        >
                                                            <FaTelegram className="text-white text-2xl" />
                                                        </a>
                                                        <a
                                                            href=""
                                                            className="bg-white/10 hover:bg-white/20 p-3 rounded-full transition-colors"
                                                            aria-label="YouTube"
                                                        >
                                                            <FaYoutube className="text-white text-2xl" />
                                                        </a>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>

                            </Zoom>
                        </div>
                )
                }
            </div >

        </div >
    );
}

export default TestAbiturient;