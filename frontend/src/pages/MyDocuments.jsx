import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';
import { Search, Sparkles, FolderOpen, Calendar, ShieldAlert } from 'lucide-react';

export default function MyDocuments() {
  const [documents, setDocuments] = useState([]);
  const [searchName, setSearchName] = useState('');
  const [searchType, setSearchType] = useState('');
  const [nlQuery, setNlQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const response = await API.get('/documents');
      setDocuments(response.data);
    } catch (err) {
      console.error('Failed to load documents', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await API.get('/documents/search', {
        params: {
          name: searchName || null,
          type: searchType || null
        }
      });
      setDocuments(response.data);
    } catch (err) {
      console.error('Search failed', err);
    } finally {
      setLoading(false);
    }
  };

  const handleNlSearch = async (e) => {
    e.preventDefault();
    if (!nlQuery.trim()) return;

    setLoading(true);
    try {
      const response = await API.get('/documents/nl-search', {
        params: { query: nlQuery }
      });
      setDocuments(response.data);
    } catch (err) {
      console.error('Natural language search failed', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-bold text-white">Document Drawer</h2>
        <p className="text-sm text-gray-400">Search and navigate through all index records inside the enterprise archive.</p>
      </div>

      {/* Search filters */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Basic criteria search */}
        <form onSubmit={handleSearch} className="p-6 rounded-xl glass-panel shadow-md space-y-4 lg:col-span-1">
          <h4 className="text-sm font-semibold text-white uppercase tracking-wider mb-2">Criteria Filter</h4>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="Filename keyword"
              className="w-full px-4 py-2.5 rounded-lg glass-input text-xs"
              value={searchName}
              onChange={(e) => setSearchName(e.target.value)}
            />
            <select
              className="w-full px-4 py-2.5 rounded-lg glass-input text-xs appearance-none"
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
            >
              <option value="" className="bg-darkBg">All Formats</option>
              <option value="PDF" className="bg-darkBg">PDF</option>
              <option value="DOCX" className="bg-darkBg">DOCX</option>
              <option value="TXT" className="bg-darkBg">TXT</option>
            </select>
          </div>
          <button
            type="submit"
            className="w-full flex items-center justify-center gap-2 py-2 rounded-lg bg-darkSurface hover:bg-gray-800 text-white border border-darkBorder font-medium text-xs transition"
          >
            <Search size={14} />
            <span>Search</span>
          </button>
        </form>

        {/* Natural language AI Search */}
        <form onSubmit={handleNlSearch} className="p-6 rounded-xl glass-panel shadow-md space-y-4 lg:col-span-2 flex flex-col justify-between">
          <div>
            <h4 className="text-sm font-semibold text-white uppercase tracking-wider mb-2">Natural Language Search</h4>
            <p className="text-xs text-gray-400 mb-3">Ask queries like: "Show invoices uploaded this month" or "Show contracts mentioning termination".</p>
            <div className="relative">
              <input
                type="text"
                placeholder="What documents are you looking for?"
                className="w-full pl-4 pr-10 py-3 rounded-lg glass-input text-xs"
                value={nlQuery}
                onChange={(e) => setNlQuery(e.target.value)}
              />
              <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-indigo-400">
                <Sparkles size={16} />
              </div>
            </div>
          </div>
          <button
            type="submit"
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg bg-brandPrimary hover:bg-indigo-700 text-white font-medium text-xs transition mt-4"
          >
            <span>Run AI Search Query</span>
          </button>
        </form>
      </div>

      {/* Document Records */}
      <div className="p-6 rounded-xl glass-panel shadow-md">
        {loading ? (
          <div className="flex h-32 items-center justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-brandPrimary"></div>
          </div>
        ) : documents.length === 0 ? (
          <div className="text-center py-8 text-gray-500 text-sm">
            <FolderOpen className="mx-auto mb-3" size={28} />
            <span>No documents match the search criteria</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-darkBorder text-gray-400 text-xs">
                  <th className="py-3">Name</th>
                  <th className="py-3">Format</th>
                  <th className="py-3">Created</th>
                  <th className="py-3">Owner</th>
                  <th className="py-3">Status</th>
                  <th className="py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-darkBorder/40 text-gray-300">
                {documents.map((doc) => (
                  <tr key={doc.id} className="hover:bg-darkSurface/30 transition">
                    <td className="py-4 font-medium max-w-[220px] truncate">{doc.name}</td>
                    <td className="py-4 text-xs font-semibold uppercase">{doc.fileType}</td>
                    <td className="py-4 text-xs text-gray-400">
                      {new Date(doc.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 text-xs text-gray-400">{doc.ownerUsername}</td>
                    <td className="py-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                          doc.status === 'COMPLETED'
                            ? 'bg-emerald-950/50 text-emerald-400 border border-emerald-500/30'
                            : doc.status === 'PROCESSING'
                            ? 'bg-yellow-950/50 text-yellow-400 border border-yellow-500/30'
                            : 'bg-red-950/50 text-red-400 border border-red-500/30'
                        }`}
                      >
                        {doc.status}
                      </span>
                    </td>
                    <td className="py-4 text-right">
                      <button
                        onClick={() => navigate(`/documents/${doc.id}`)}
                        className="px-3.5 py-1.5 bg-darkSurface border border-darkBorder hover:border-brandPrimary hover:text-white rounded-md text-xs font-medium transition"
                      >
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
