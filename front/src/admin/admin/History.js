import React, { useState, useEffect } from "react";
import Sidebar from "./Sidebar";
import ApiCall, { baseUrl } from "../../config";

const getTodayDate = () => {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

function History() {
  const [history, setHistory] = useState([]);
  const [filters, setFilters] = useState({
    createdAt: getTodayDate(),
  });
  const [isLoading, setIsLoading] = useState(false);

  const fetchHistory = async () => {
    setIsLoading(true);
    try {
      const queryParams = new URLSearchParams();
      if (filters.createdAt) {
        queryParams.append("createdAt", filters.createdAt);
      }
      const response = await ApiCall(
        `/api/v1/history-of-abuturient?${queryParams.toString()}`,
        "GET",
        null,
        null,
        true
      );

      setHistory(response.data);
    } catch (error) {
      console.error("Error fetching appeals:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString("uz-UZ", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <Sidebar />
      <div className="p-6 sm:ml-64">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6 gap-4">
          <h2 className="text-3xl font-bold text-gray-800">Kunlik Faollik</h2>
          <div className="flex items-center gap-4">
            <div className="relative">
              <input
                type="date"
                name="createdAt"
                value={filters.createdAt}
                onChange={handleFilterChange}
                max={getTodayDate()}
                className="border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
              />
            </div>
            <button
              onClick={fetchHistory}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all"
            >
              Ko'rish
            </button>
          </div>
        </div>

        {/* Jadval qismi */}
        <div className="bg-white rounded-lg shadow border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead className="bg-blue-600 text-white">
                <tr>
                  <th className="px-2 py-2 text-left font-medium">№</th>
                  <th className="px-2 py-2 text-left font-medium">F.I.Sh</th>
                  <th className="px-2 py-2 text-left font-medium">Tel</th>
                  <th className="px-2 py-2 text-left font-medium">Yo'nalish</th>
                  <th className="px-2 py-2 text-left font-medium">Shakli</th>
                  <th className="px-2 py-2 text-left font-medium">Ball</th>
                  <th className="px-2 py-2 text-left font-medium">Agent</th>
                  <th className="px-2 py-2 text-left font-medium">
                    Ariza turi
                  </th>
                  <th className="px-2 py-2 text-left font-medium">Vaqt</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {isLoading ? (
                  <tr>
                    <td colSpan="8" className="px-2 py-4 text-center">
                      <div className="flex justify-center">
                        <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-blue-500"></div>
                      </div>
                    </td>
                  </tr>
                ) : history.length > 0 ? (
                  history.map((item, index) => (
                    <tr key={item?.id} className="hover:bg-gray-50">
                      <td className="px-2 py-2 whitespace-nowrap font-medium">
                        {index + 1}
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap truncate max-w-[120px]">
                        {`${item?.abuturient?.lastName} ${item?.abuturient?.firstName}`}
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap">
                        {item?.abuturient?.phone}
                      </td>
                      <td className="px-2 py-2 truncate max-w-[100px]">
                        {item?.abuturient?.educationField?.name}
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap">
                        <span className="px-1.5 py-0.5 text-[0.65rem] font-semibold rounded-full bg-blue-100 text-blue-800">
                          {
                            item?.abuturient?.educationField?.educationForm
                              ?.name
                          }
                        </span>
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap">
                        <span className="px-1.5 py-0.5 text-[0.65rem] font-semibold rounded-full bg-green-100 text-green-800">
                          {item?.abuturient?.ball}
                        </span>
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap">
                        <span className="px-1.5 py-0.5 text-[0.65rem] font-semibold rounded-full bg-yellow-100 text-green-800">
                          {item?.abuturient?.agent?.name}
                        </span>
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap truncate max-w-[80px]">
                        {item?.abuturient?.appealType?.name}
                      </td>
                      <td className="px-2 py-2 whitespace-nowrap">
                        {new Date(item.date).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan="8"
                      className="px-2 py-6 text-center text-gray-500"
                    >
                      <div className="flex flex-col items-center">
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          className="h-8 w-8 text-gray-400"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                          />
                        </svg>
                        <p className="mt-1 text-sm font-medium">
                          Ma'lumot yo'q
                        </p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export default History;
