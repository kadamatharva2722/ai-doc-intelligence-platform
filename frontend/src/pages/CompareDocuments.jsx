import React, { useEffect, useState } from 'react';
import API from '../services/api';
import { GitCompare, AlertCircle, RefreshCw } from 'lucide-react';

export default function CompareDocuments() {
  const [documents, setDocuments] = useState([]);
  const [doc1Id, setDoc1Id] = useState('');
  const [doc2Id, setDoc2Id] = useState('');
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchDocs = async () => {
      try {
        const response = await API.get('/documents');
        // Only allow completed documents to be compared
        const completedDocs = response.data.filter((d) => d.status === 'COMPLETED');
        setDocuments(completedDocs);
      } catch (err) {
        console.error('Failed to fetch documents', err);
      }
    };
    fetchDocs();
  }, []);

  const handleCompare = async (e) => {
    e.preventDefault();
    if (!doc1Id || !doc2Id) {
      setError('Please select two documents to compare');
      return;
    }
    if (doc1Id === doc2Id) {
      setError('Please select two different documents to compare');
      return;
    }

    setError('');
    setLoading(true);
    setReport(null);

    try {
      const response = await API.post('/analysis/compare', {
        documentId1: doc1Id,
        documentId2: doc2Id,
      });
      setReport(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Comparison failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-bold text-white">Compare Documents</h2>
        <p className="text-sm text-gray-400">Select two documents to compare versions, clauses, prices, or parameters.</p>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-900/30 border border-red-500/50 text-red-200 text-sm max-w-3xl">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Select Box Block */}
      <form onSubmit={handleCompare} className="p-6 rounded-xl glass-panel shadow-md grid grid-cols-1 md:grid-cols-3 gap-6 items-end max-w-3xl">
        <div className="space-y-2">
          <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider">Version 1</label>
          <select
            className="w-full px-4 py-2.5 rounded-lg glass-input text-xs appearance-none"
            value={doc1Id}
            onChange={(e) => setDoc1Id(e.target.value)}
          >
            <option value="" className="bg-darkBg">Select document...</option>
            {documents.map((doc) => (
              <option key={doc.id} value={doc.id} className="bg-darkBg">{doc.name}</option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider">Version 2</label>
          <select
            className="w-full px-4 py-2.5 rounded-lg glass-input text-xs appearance-none"
            value={doc2Id}
            onChange={(e) => setDoc2Id(e.target.value)}
          >
            <option value="" className="bg-darkBg">Select document...</option>
            {documents.map((doc) => (
              <option key={doc.id} value={doc.id} className="bg-darkBg">{doc.name}</option>
            ))}
          </select>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg bg-brandPrimary hover:bg-indigo-700 text-white font-medium text-xs transition disabled:opacity-50"
        >
          {loading ? (
            <>
              <RefreshCw className="animate-spin" size={14} />
              <span>Comparing...</span>
            </>
          ) : (
            <>
              <GitCompare size={14} />
              <span>Run Comparison</span>
            </>
          )}
        </button>
      </form>

      {/* Comparison Results */}
      {report && (
        <div className="p-6 rounded-xl glass-panel shadow-md space-y-6 max-w-3xl">
          <div className="border-b border-darkBorder pb-4">
            <h3 className="font-bold text-white text-base">Comparison Report</h3>
            <p className="text-xs text-gray-400 mt-1">
              Comparing <strong>{report.doc1Name}</strong> with <strong>{report.doc2Name}</strong>
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Findings Summary</h4>
              <p className="text-xs text-gray-300 leading-relaxed whitespace-pre-wrap">{report.differences}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
