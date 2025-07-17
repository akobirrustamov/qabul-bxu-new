import React, { useState, useEffect } from 'react';
import ApiCall, { baseUrl } from "../../config";
import Sidebar from "./Sidebar";
import {
    LineChart,
    Line,
    CartesianGrid,
    XAxis,
    YAxis,
    Tooltip,
    ResponsiveContainer,
    BarChart,
    Bar
} from 'recharts';
import * as XLSX from "xlsx";
import jsPDF from "jspdf";
import "jspdf-autotable";
import Select from 'react-select';
import { motion } from 'framer-motion';
import { FiDownload, FiFilter, FiRefreshCw, FiTrendingUp, FiUsers, FiUser, FiActivity } from 'react-icons/fi';

// Animatsiya variantlari
const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: {
            staggerChildren: 0.1
        }
    }
};

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: {
        y: 0,
        opacity: 1,
        transition: {
            duration: 0.5
        }
    }
};

const cardVariants = {
    hidden: { scale: 0.9, opacity: 0 },
    visible: {
        scale: 1,
        opacity: 1,
        transition: {
            duration: 0.5
        }
    }
};

const loadingVariants = {
    animate: {
        rotate: 360,
        transition: {
            duration: 1,
            repeat: Infinity,
            ease: "linear"
        }
    }
};

const AdminHome = () => {
    const [statistics, setStatistics] = useState([]);
    const [dailyStats, setDailyStats] = useState([]);
    const [sortOrder, setSortOrder] = useState("desc");
    const [loading, setLoading] = useState(false);
    const [agentDailyStats, setAgentDailyStats] = useState([]);
    const [selectedAgent, setSelectedAgent] = useState([]); // not array

    const [agents, setAgents] = useState([]);
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [isRefreshing, setIsRefreshing] = useState(false);

    useEffect(() => {
        fetchAllData();
    }, []);
    const fetchAllData = async () => {
        setLoading(true);
        setIsRefreshing(true);
        try {
            await Promise.all([
                fetchStatistics(),
                fetchDailyStats(),
                fetchAgentDailyStats(),
                fetchAgents()
            ]);
        } catch (error) {
            console.error("Ma'lumotlarni yuklashda xato:", error);
        } finally {
            setLoading(false);
            setIsRefreshing(false);
        }
    };

    const fetchAgents = async () => {
        try {
            const response = await ApiCall("/api/v1/agent", "GET", null, null, true);
            setAgents(response.data);

        } catch (error) {
            console.error("Agentlarni yuklashda xato:", error);
        }
    };



    const fetchAgentDailyStats = async (agentPhone = "all") => {
        try {
            const response = await ApiCall('/api/v1/agent/daily-agent-statistic', 'GET', null, null, true);
            const allData = response.data;

            if (agentPhone === "all") {
                setAgentDailyStats(allData);
            } else {
                const filtered = allData.filter(agent => agent.phone === agentPhone);
                setAgentDailyStats(filtered);
            }
        } catch (error) {
            console.error("Agentlarning kunlik statistikasini yuklashda xato:", error);
        }
    };

    const fetchStatistics = async () => {
        try {
            const response = await ApiCall('/api/v1/agent/statistic', 'GET', null, null, true);
            setStatistics(response.data);
        } catch (error) {
            console.error("Statistika yuklashda xato:", error);
        }
    };

    const fetchDailyStats = async () => {
        try {
            const response = await ApiCall('/api/v1/agent/daily-statistic', 'GET', null, null, true);
            const formatted = Object.entries(response.data).map(([date, count]) => ({
                date,
                count,
            }));

            const sorted = formatted.sort((a, b) => {
                const dateA = new Date(a.date);
                const dateB = new Date(b.date);
                return sortOrder === "asc" ? dateA - dateB : dateB - dateA;
            });

            setDailyStats(sorted);
        } catch (error) {
            console.error("Kunlik statistikani yuklashda xato:", error);
        }
    };





    const handleSortChange = (e) => {
        setSortOrder(e.target.value);
        fetchDailyStats(); // Yangi tartibda qayta yuklash
    };

    const exportToExcel = () => {
        const ws = XLSX.utils.json_to_sheet(dailyStats);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, "Kunlik statistika");
        XLSX.writeFile(wb, "kunlik_statistika.xlsx");
    };

    const exportToPDF = () => {
        const doc = new jsPDF();
        doc.setFontSize(14);
        doc.text("📊 Kunlik ro'yxatdan o'tish statistikasi", 14, 20);

        const tableData = dailyStats.map((entry) => [entry.date, entry.count]);

        doc.autoTable({
            startY: 30,
            head: [["Sana", "Ro'yxatdan o'tishlar soni"]],
            body: tableData,
        });

        doc.save("kunlik_statistika.pdf");
    };

    const exportAgentsToExcel = (agents, filename) => {
        if (agents.length === 0) return;

        const formatted = agents.map((agent, index) => ({
            "#": index + 1,
            "Agent nomi": agent.name,
            "Telefon raqami": agent.phone,
            "Arizalar soni": agent.count,
        }));

        const ws = XLSX.utils.json_to_sheet(formatted);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, "Agentlar");
        XLSX.writeFile(wb, filename);
    };

    const totalCount = statistics.reduce((sum, agent) => sum + agent.count, 0);
    const isBotAgent = (phone) => /^[0-9]+$/.test(phone);
    const botAgents = statistics.filter(agent => isBotAgent(agent.phone));
    const siteAgents = statistics.filter(agent => !isBotAgent(agent.phone));

    // Sana hisob-kitoblari
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const dayBeforeYesterday = new Date(yesterday);
    dayBeforeYesterday.setDate(dayBeforeYesterday.getDate() - 1);

    const formatDate = (date) => date.toISOString().split('T')[0];
    const todayStr = formatDate(today);
    const yesterdayStr = formatDate(yesterday);
    const dayBeforeYesterdayStr = formatDate(dayBeforeYesterday);

    const ALL_OPTION = { value: "*", label: "Barchasi" };

    const agentOptions = [
        ALL_OPTION,
        { value: null, label: "Universitet" },
        ...agents.map((agent) => ({
            value: agent?.agent.id,
            label: `${agent.agent.name}`,
        })),
    ];



    // const handleDownloadFilteredReport = async () => {
    //     const selectedId = selectedAgent?.value;
    //
    //     setLoading(true); // Loading holatini yoqamiz
    //
    //     const obj = {
    //         adminId: selectedId !== null ? selectedId : null,
    //         startDate: startDate,
    //         endDate: endDate,
    //         university: selectedId === null
    //     };
    //
    //     try {
    //         const token = localStorage.getItem("access_token");
    //
    //         const response = await fetch(`${baseUrl}/api/v1/data-manager/statistic`, {
    //             method: "POST",
    //             headers: {
    //                 "Content-Type": "application/json",
    //                 Authorization: `Bearer ${token}`,
    //             },
    //             body: JSON.stringify(obj),
    //         });
    //
    //         if (!response.ok) {
    //             throw new Error("Server error");
    //         }
    //
    //         const blob = await response.blob();
    //         const url = window.URL.createObjectURL(blob);
    //         const a = document.createElement("a");
    //         a.href = url;
    //         a.download = "statistika.xlsx";
    //         document.body.appendChild(a);
    //         a.click();
    //         a.remove();
    //         window.URL.revokeObjectURL(url);
    //     } catch (error) {
    //         console.error("Statistikani olishda xatolik:", error);
    //         alert("Hisobot yuklab olishda xatolik yuz berdi. Iltimos, qayta urunib ko'ring.");
    //     } finally {
    //         setLoading(false); // Loading holatini o'chiramiz
    //     }
    // };
    //
    const handleDownloadFilteredReport = async () => {
        const selectedIds = selectedAgent?.map(agent => agent.value) || [];

        setLoading(true);

        const obj = {
            adminIds: selectedIds.length > 0 ? selectedIds : null,
            startDate: startDate,
            endDate: endDate,
            university: selectedIds.includes(null) // Check if "Universitet" is selected
        };

        try {
            const token = localStorage.getItem("access_token");

            const response = await fetch(`${baseUrl}/api/v1/data-manager/statistic`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(obj),
            });

            if (!response.ok) {
                throw new Error("Server error");
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "statistika.xlsx";
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Statistikani olishda xatolik:", error);
            alert("Hisobot yuklab olishda xatolik yuz berdi. Iltimos, qayta urunib ko'ring.");
        } finally {
            setLoading(false);
        }
    };

    const LoadingSpinner = () => (
        <motion.div
            variants={loadingVariants}
            animate="animate"
            className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full"
        />
    );

    const renderTable = (title, agents, type) => (
        <motion.div
            variants={cardVariants}
            className="bg-white rounded-xl shadow-md p-6 mb-6 border border-gray-100"
        >
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
                    {type === 'bot' ? (
                        <>
                            <FiActivity className="text-purple-500" /> Bot Agentlar
                        </>
                    ) : (
                        <>
                            <FiUsers className="text-blue-500" /> Sayt Agentlari
                        </>
                    )}
                    <span className="text-sm font-normal bg-blue-100 text-blue-800 px-2 py-1 rounded-full">
                        {agents.length} agent
                    </span>
                </h2>
                <button
                    onClick={() => exportAgentsToExcel(agents, `${type}_agentlar.xlsx`)}
                    className="flex items-center gap-1 bg-gradient-to-r from-blue-500 to-blue-600 text-white px-3 py-1 rounded-md text-sm hover:from-blue-600 hover:to-blue-700 transition-all"
                >
                    <FiDownload size={14} /> Yuklab olish
                </button>
            </div>

            {agents.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                    Ma'lumot mavjud emas
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <div className="overflow-y-auto max-h-[500px] rounded-lg border border-gray-200">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">#</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Agent nomi</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Telefon</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Jami</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{dayBeforeYesterdayStr}</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{yesterdayStr}</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{todayStr}</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {agents.map((agent, index) => {
                                    const agentDaily = agentDailyStats.find(a => a.phone === agent.phone)?.daily || {};
                                    const dayBeforeYesterdayCount = agentDaily[dayBeforeYesterdayStr] || 0;
                                    const yesterdayCount = agentDaily[yesterdayStr] || 0;
                                    const todayCount = agentDaily[todayStr] || 0;

                                    return (
                                        <tr key={index} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">{index + 1}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">{agent.name}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">{agent.phone}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-blue-600">{agent.count}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">{dayBeforeYesterdayCount}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">{yesterdayCount}</td>
                                            <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-green-600">{todayCount}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </motion.div>
    );

    const renderDailyStatsTable = () => (
        <motion.div
            variants={cardVariants}
            className="bg-white rounded-xl shadow-md p-6 mb-6 border border-gray-100"
        >
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
                    <FiTrendingUp className="text-green-500" /> Kunlik ro'yxatdan o'tish statistikasi
                </h2>
                <div className="flex gap-2">
                    <select
                        className="border border-gray-300 px-3 py-1 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        value={sortOrder}
                        onChange={handleSortChange}
                    >
                        <option value="desc">🆕 Yangilari birinchi</option>
                        <option value="asc">📅 Eski birinchi</option>
                    </select>
                    <button
                        onClick={exportToExcel}
                        className="flex items-center gap-1 bg-gradient-to-r from-green-500 to-green-600 text-white px-3 py-1 rounded-md text-sm hover:from-green-600 hover:to-green-700 transition-all"
                    >
                        <FiDownload size={14} /> Excel
                    </button>
                    <button
                        onClick={exportToPDF}
                        className="flex items-center gap-1 bg-gradient-to-r from-red-500 to-red-600 text-white px-3 py-1 rounded-md text-sm hover:from-red-600 hover:to-red-700 transition-all"
                    >
                        <FiDownload size={14} /> PDF
                    </button>
                </div>
            </div>

            {dailyStats.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                    Kunlik statistika mavjud emas
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <div className="overflow-y-auto max-h-[500px] rounded-lg border border-gray-200">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Sana</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ro'yxatdan o'tishlar soni</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {dailyStats.map((entry, index) => (
                                    <tr key={index} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">{entry.date}</td>
                                        <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-blue-600">{entry.count}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </motion.div>
    );

    return (
        <div className="min-h-screen bg-gray-50">
            <Sidebar />

            {loading ? (
                <div className="flex items-center justify-center h-screen">
                    <div className="flex flex-col items-center gap-4">
                        <LoadingSpinner />
                        <p className="text-gray-600">Boshqaruv paneli yuklanmoqda...</p>
                    </div>
                </div>
            ) : (
                <motion.div
                    initial="hidden"
                    animate="visible"
                    variants={containerVariants}
                    className="p-6 sm:ml-64"
                >
                    <div className="flex justify-between items-center mb-8">
                        <motion.h1 variants={itemVariants} className="text-3xl font-bold text-gray-800">
                            Boshqaruv Paneli
                        </motion.h1>
                        <motion.button
                            variants={itemVariants}
                            onClick={fetchAllData}
                            className="flex items-center gap-2 bg-white border border-gray-300 px-4 py-2 rounded-lg shadow-sm hover:bg-gray-50 transition-colors"
                            disabled={isRefreshing}
                        >
                            {isRefreshing ? (
                                <LoadingSpinner />
                            ) : (
                                <FiRefreshCw size={18} />
                            )}
                            Ma'lumotlarni yangilash
                        </motion.button>
                    </div>

                    {/* Qisqacha ma'lumot kartalari */}
                    <motion.div
                        variants={containerVariants}
                        className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8"
                    >
                        <motion.div variants={itemVariants} className="bg-white p-6 rounded-xl shadow-md border-l-4 border-blue-500">
                            <div className="flex justify-between items-start">
                                <div>
                                    <p className="text-sm font-medium text-gray-500">Jami arizalar</p>
                                    <p className="text-3xl font-bold text-gray-800 mt-1">{totalCount}</p>
                                </div>
                                <div className="bg-blue-100 p-3 rounded-full">
                                    <FiUser className="text-blue-600" size={20} />
                                </div>
                            </div>
                        </motion.div>

                        <motion.div variants={itemVariants} className="bg-white p-6 rounded-xl shadow-md border-l-4 border-green-500">
                            <div className="flex justify-between items-start">
                                <div>
                                    <p className="text-sm font-medium text-gray-500">Sayt Agentlari</p>
                                    <p className="text-3xl font-bold text-gray-800 mt-1">{siteAgents.length}</p>
                                </div>
                                <div className="bg-green-100 p-3 rounded-full">
                                    <FiUsers className="text-green-600" size={20} />
                                </div>
                            </div>
                        </motion.div>

                        <motion.div variants={itemVariants} className="bg-white p-6 rounded-xl shadow-md border-l-4 border-purple-500">
                            <div className="flex justify-between items-start">
                                <div>
                                    <p className="text-sm font-medium text-gray-500">Bot Agentlar</p>
                                    <p className="text-3xl font-bold text-gray-800 mt-1">{botAgents.length}</p>
                                </div>
                                <div className="bg-purple-100 p-3 rounded-full">
                                    <FiActivity className="text-purple-600" size={20} />
                                </div>
                            </div>
                        </motion.div>
                    </motion.div>

                    {/* Hisobot generatori */}
                    <motion.div
                        variants={cardVariants}
                        className="bg-white rounded-xl shadow-md p-6 mb-8 border border-gray-100"
                    >
                        <h2 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
                            <FiFilter className="text-orange-500" /> Maxsus hisobot yaratish
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Agentni tanlang</label>

                                <Select
                                    options={agentOptions}
                                    value={selectedAgent} // Now this will be an array
                                    onChange={(selectedOptions) => {
                                        setSelectedAgent(selectedOptions || []); // Store array of selected options
                                        setEndDate("");
                                    }}
                                    isMulti // Enable multiple selection
                                    placeholder="Agentni tanlang"
                                    isSearchable
                                    className="basic-multi-select"
                                    classNamePrefix="select"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Boshlanish sanasi</label>
                                <input
                                    type="date"
                                    className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    onChange={(e) => {
                                        const selected = e.target.value;
                                        const today = new Date().toISOString().split('T')[0];
                                        if (selected > today) {
                                            alert("Kelajakdagi sanani tanlash mumkin emas.");
                                            e.target.value = "";
                                            setStartDate("");
                                        } else {
                                            setStartDate(selected);
                                            setEndDate("");
                                        }
                                    }}
                                    max={new Date().toISOString().split('T')[0]}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Tugash sanasi (ixtiyoriy)</label>
                                <input
                                    type="date"
                                    className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    disabled={!startDate}
                                    value={endDate}
                                    onChange={(e) => {
                                        const selectedEnd = e.target.value;
                                        const today = new Date().toISOString().split('T')[0];
                                        if (startDate === selectedEnd) {
                                            alert("Boshlanish va tugash sanasi bir xil bo'lishi mumkin emas.");
                                            return;
                                        }
                                        if (selectedEnd < startDate || selectedEnd > today) {
                                            alert("Tugash sanasi boshlanish sanasidan keyin va bugungi kunga teng yoki kichik bo'lishi kerak.");
                                            return;
                                        }
                                        setEndDate(selectedEnd);
                                    }}
                                    min={startDate}
                                    max={new Date().toISOString().split('T')[0]}
                                />
                            </div>
                            <button
                                onClick={handleDownloadFilteredReport}
                                disabled={selectedAgent.length === 0 || !startDate || loading}
                                className={`px-4 py-2 rounded-md flex items-center justify-center gap-2 transition-all ${!selectedAgent || !startDate || loading
                                    ? 'bg-gray-300 text-gray-700 cursor-not-allowed'
                                    : 'bg-gradient-to-r from-orange-500 to-orange-600 text-white hover:from-orange-600 hover:to-orange-700'
                                    }`}
                            >
                                {loading ? (
                                    <>
                                        <motion.div
                                            animate={{ rotate: 360 }}
                                            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                                            className="w-4 h-4 border-2 border-white border-t-transparent rounded-full"
                                        />
                                        Yuklanmoqda...
                                    </>
                                ) : (
                                    <>
                                        <FiDownload size={16} /> Hisobot yaratish
                                    </>
                                )}
                            </button>
                        </div>
                    </motion.div>


                    {/* Grafiklar bo'limi */}
                    <motion.div variants={cardVariants} className="bg-white rounded-xl shadow-md p-6 mb-8 border border-gray-100">
                        <h2 className="text-xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
                            <FiTrendingUp className="text-blue-500" /> Ro'yxatdan o'tish analitikasi
                        </h2>
                        <div className="grid grid-cols-1 lg:grid-cols-1 gap-6">
                            <div>
                                <h3 className="text-lg font-medium text-gray-700 mb-4">Kunlik tendentsiya</h3>
                                <div className="h-80">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <LineChart data={dailyStats}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                            <XAxis dataKey="date" />
                                            <YAxis allowDecimals={false} />
                                            <Tooltip
                                                contentStyle={{
                                                    backgroundColor: 'white',
                                                    borderRadius: '8px',
                                                    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
                                                    border: 'none'
                                                }}
                                            />
                                            <Line
                                                type="monotone"
                                                dataKey="count"
                                                stroke="#4f46e5"
                                                strokeWidth={2}
                                                dot={{ r: 4 }}
                                                activeDot={{ r: 6, stroke: '#4f46e5', strokeWidth: 2 }}
                                            />
                                        </LineChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>

                        </div>
                    </motion.div>

                    {/* Agentlar jadvallari */}
                    {renderTable("", siteAgents, 'site')}
                    {renderTable("", botAgents, 'bot')}

                    {/* Kunlik statistika jadvali */}
                    {renderDailyStatsTable()}
                </motion.div>
            )}
        </div>
    );
};

export default AdminHome;