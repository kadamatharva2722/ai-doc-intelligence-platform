import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';
import { Files, Zap, Upload, Clock, FileText, CheckCircle2, GitCompare } from 'lucide-react';

export default function Dashboard() {
  const [stats, setStats] = useState({ totalDocuments: 0, todayUploads: 0, totalAiRequests: 0 });
  const [recentDocs, setRecentDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const statsRes = await API.get('/analysis/dashboard');
        setStats(statsRes.data);

        const docsRes = await API.get('/documents');
        setRecentDocs(docsRes.data.slice(-5).reverse());
      } catch (err) {
        console.error('Failed to load dashboard statistics', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brandPrimary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-6 rounded-xl glass-panel shadow-md flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-400 mb-1">Total Documents</p>
            <h3 className="text-3xl font-bold text-white">{stats.totalDocuments}</h3>
          </div>
          <div className="p-3 rounded-lg bg-indigo-950/50 border border-indigo-500/30 text-indigo-400">
            <Files size={24} />
          </div>
        </div>

        <div className="p-6 rounded-xl glass-panel shadow-md flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-400 mb-1">Today's Uploads</p>
            <h3 className="text-3xl font-bold text-white">{stats.todayUploads}</h3>
          </div>
          <div className="p-3 rounded-lg bg-indigo-950/50 border border-indigo-500/30 text-indigo-400">
            <Upload size={24} />
          </div>
        </div>

        <div className="p-6 rounded-xl glass-panel shadow-md flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-400 mb-1">AI Requests Processed</p>
            <h3 className="text-3xl font-bold text-white">{stats.totalAiRequests}</h3>
          </div>
          <div className="p-3 rounded-lg bg-indigo-950/50 border border-indigo-500/30 text-indigo-400">
            <Zap size={24} />
          </div>
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Recent Uploads Table */}
        <div className="lg:col-span-2 p-6 rounded-xl glass-panel shadow-md space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-lg font-bold text-white">Recent Documents</h4>
            <button
              onClick={() => navigate('/documents')}
              className="text-xs font-semibold text-brandPrimary hover:text-indigo-400"
            >
              View All
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-darkBorder text-gray-400 text-xs">
                  <th className="py-3">Name</th>
                  <th className="py-3">Type</th>
                  <th className="py-3">Status</th>
                  <th className="py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-darkBorder/40">
                {recentDocs.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="py-4 text-center text-gray-500">
                      No documents uploaded yet
                    </td>
                  </tr>
                ) : (
                  recentDocs.map((doc) => (
                    <tr key={doc.id} className="text-gray-300 hover:bg-darkSurface/30 transition">
                      <td className="py-4 font-medium max-w-[200px] truncate">{doc.name}</td>
                      <td className="py-4 text-xs font-semibold uppercase">{doc.fileType}</td>
                      <td className="py-4">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            doc.status === 'COMPLETED'
                              ? 'bg-emerald-950/50 text-emerald-400 border border-emerald-500/30'
                              : doc.status === 'PROCESSING'
                              ? 'bg-yellow-950/50 text-yellow-400 border border-yellow-500/30'
                              : 'bg-gray-950/50 text-gray-400 border border-gray-500/30'
                          }`}
                        >
                          {doc.status}
                        </span>
                      </td>
                      <td className="py-4 text-right">
                        <button
                          onClick={() => navigate(`/documents/${doc.id}`)}
                          className="px-3 py-1 bg-darkSurface border border-darkBorder hover:border-brandPrimary hover:text-white rounded-md text-xs transition"
                        >
                          Analyze
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* AI Action Panel */}
        <div className="p-6 rounded-xl glass-panel shadow-md flex flex-col justify-between">
          <div className="space-y-4">
            <h4 className="text-lg font-bold text-white">Document Tasks</h4>
            <p className="text-sm text-gray-400 leading-relaxed">
              Upload PDF contracts, financial invoices, or salary slips to instantly extract attributes, generate highlights, and compare iterations.
            </p>
          </div>

          <div className="space-y-3 mt-6">
            <button
              onClick={() => navigate('/upload')}
              className="w-full flex items-center justify-center gap-2 py-3 rounded-lg bg-brandPrimary hover:bg-indigo-700 text-white font-medium text-sm transition"
            >
              <Upload size={16} />
              <span>Upload New File</span>
            </button>
            <button
              onClick={() => navigate('/compare')}
              className="w-full flex items-center justify-center gap-2 py-3 rounded-lg bg-darkSurface hover:bg-gray-800 text-gray-300 font-medium text-sm border border-darkBorder transition"
            >
              <GitCompare size={16} />
              <span>Compare Versions</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
