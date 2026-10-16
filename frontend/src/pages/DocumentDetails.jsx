import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import API from '../services/api';
import { Send, FileText, Bot, AlertTriangle, Key, ChevronRight } from 'lucide-react';

export default function DocumentDetails() {
  const { id } = useParams();
  const [doc, setDoc] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [chatQuestion, setChatQuestion] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [chatLoading, setChatLoading] = useState(false);

  useEffect(() => {
    const fetchDetails = async () => {
      try {
        const docRes = await API.get(`/documents/${id}`);
        setDoc(docRes.data);

        if (docRes.data.status === 'COMPLETED') {
          const analysisRes = await API.get(`/analysis/${id}`);
          setAnalysis(analysisRes.data);

          const chatRes = await API.get(`/analysis/${id}/chat`);
          setChatHistory(chatRes.data);
        }
      } catch (err) {
        console.error('Failed to load document details', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDetails();
  }, [id]);

  const handleChat = async (e) => {
    e.preventDefault();
    if (!chatQuestion.trim()) return;

    setChatLoading(true);
    const tempQuestion = chatQuestion;
    setChatQuestion('');

    try {
      const response = await API.post(`/analysis/${id}/chat`, { question: tempQuestion });
      setChatHistory((prev) => [...prev, response.data]);
    } catch (err) {
      console.error('Failed to ask AI', err);
    } finally {
      setChatLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brandPrimary"></div>
      </div>
    );
  }

  if (!doc) {
    return <div className="text-center text-gray-400">Document not found</div>;
  }

  // Parse structured data JSON safely and clean markdown code blocks
  let parsedMetadata = {};
  if (analysis?.extractedData) {
    try {
      let rawJson = analysis.extractedData.trim();
      if (rawJson.startsWith('```json')) {
        rawJson = rawJson.substring(7);
      } else if (rawJson.startsWith('```')) {
        rawJson = rawJson.substring(3);
      }
      if (rawJson.endsWith('```')) {
        rawJson = rawJson.substring(0, rawJson.length - 3);
      }
      parsedMetadata = JSON.parse(rawJson.trim());
    } catch (e) {
      parsedMetadata = { "Document Content": analysis.extractedData };
    }
  }

  // Helper to format keys like "due_date" or "totalAmount" into "Due Date" or "Total Amount"
  const formatKeyName = (key) => {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/_/g, ' ')
      .trim()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  // Helper to render complex attribute values
  const renderAttributeValue = (val) => {
    if (val === null || val === undefined) {
      return <span className="text-gray-500 italic">N/A</span>;
    }
    if (typeof val === 'boolean') {
      return (
        <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${val ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-500/30' : 'bg-rose-950/60 text-rose-400 border border-rose-500/30'}`}>
          {val ? 'Yes' : 'No'}
        </span>
      );
    }
    if (Array.isArray(val)) {
      if (val.length === 0) return <span className="text-gray-500 italic">None</span>;
      return (
        <div className="flex flex-wrap gap-1.5 justify-end max-w-xs">
          {val.map((item, idx) => (
            <span key={idx} className="px-2 py-0.5 rounded-md bg-indigo-950/40 text-indigo-300 border border-indigo-500/20 text-[11px]">
              {typeof item === 'object' ? JSON.stringify(item) : String(item)}
            </span>
          ))}
        </div>
      );
    }
    if (typeof val === 'object') {
      return (
        <div className="space-y-1 text-right max-w-xs">
          {Object.entries(val).map(([subK, subV]) => (
            <div key={subK} className="text-[11px]">
              <span className="text-gray-400">{formatKeyName(subK)}: </span>
              <span className="text-gray-200 font-medium">{String(subV)}</span>
            </div>
          ))}
        </div>
      );
    }
    return <span className="text-gray-200 font-medium text-right max-w-xs break-words">{String(val)}</span>;
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      {/* Document Analysis Info */}
      <div className="lg:col-span-2 space-y-6">
        {/* Header Summary */}
        <div className="p-6 rounded-xl glass-panel shadow-md space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xl font-bold text-white max-w-md truncate">{doc.name}</h3>
            <span className="text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full bg-brandPrimary/20 text-indigo-300 border border-indigo-500/20">
              {doc.fileType}
            </span>
          </div>
          <p className="text-xs text-gray-400">
            Uploaded on {new Date(doc.createdAt).toLocaleString()} by {doc.ownerUsername}
          </p>

          {doc.status !== 'COMPLETED' ? (
            <div className="p-4 rounded-lg bg-yellow-950/30 border border-yellow-500/30 text-yellow-300 text-xs">
              This document is currently {doc.status}. Please refresh the page in a few moments.
            </div>
          ) : (
            <div className="space-y-4 pt-4 border-t border-darkBorder">
              <div>
                <h4 className="text-sm font-semibold text-white uppercase tracking-wider mb-2">Executive Summary</h4>
                <p className="text-xs text-gray-300 leading-relaxed">{analysis.summary}</p>
              </div>
            </div>
          )}
        </div>

        {/* Structured Parameter Extraction & Key Risks */}
        {doc.status === 'COMPLETED' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Extracted JSON */}
            <div className="p-6 rounded-xl glass-panel shadow-md space-y-4">
              <div className="flex items-center gap-2 text-indigo-400 border-b border-darkBorder pb-3">
                <Key size={16} />
                <h4 className="text-sm font-semibold text-white uppercase tracking-wider">Extracted Attributes</h4>
              </div>
              <div className="space-y-3 max-h-[350px] overflow-y-auto pr-1 custom-scrollbar">
                {Object.keys(parsedMetadata).length === 0 ? (
                  <p className="text-xs text-gray-500">No parameters extracted</p>
                ) : (
                  Object.entries(parsedMetadata).map(([key, val]) => (
                    <div key={key} className="flex justify-between items-start gap-4 text-xs border-b border-darkBorder/30 pb-2.5 pt-1">
                      <span className="font-semibold text-gray-400">{formatKeyName(key)}</span>
                      {renderAttributeValue(val)}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Inconsistencies & Confidence */}
            <div className="p-6 rounded-xl glass-panel shadow-md space-y-4 flex flex-col justify-between">
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-yellow-400 border-b border-darkBorder pb-3">
                  <AlertTriangle size={18} />
                  <h4 className="text-sm font-semibold text-white uppercase tracking-wider">System Audit Remarks</h4>
                </div>
                <p className="text-xs text-gray-300 leading-relaxed whitespace-pre-line max-h-[260px] overflow-y-auto">
                  {analysis.possibleIssues || 'No remarks recorded.'}
                </p>
              </div>

              <div className="pt-4 border-t border-darkBorder mt-4 flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-400">AI Confidence Score</span>
                <span className="text-sm font-bold text-indigo-400">
                  {(analysis.confidenceScore * 100).toFixed(0)}%
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* AI Chat Side Pane */}
      <div className="lg:col-span-1 p-6 rounded-xl glass-panel shadow-md flex flex-col h-[550px] justify-between">
        <div className="border-b border-darkBorder pb-4 flex items-center gap-2">
          <Bot className="text-indigo-400" size={20} />
          <h4 className="text-sm font-semibold text-white uppercase tracking-wider">Ask AI Advisor</h4>
        </div>

        {/* Chat History Panel */}
        <div className="flex-1 overflow-y-auto my-4 space-y-4 pr-1 text-xs">
          {chatHistory.length === 0 ? (
            <div className="h-full flex items-center justify-center text-center text-gray-500">
              <p>Ask questions like:<br />"What is the total billing amount?" or "Who are the contract parties?"</p>
            </div>
          ) : (
            chatHistory.map((chat, idx) => (
              <div key={idx} className="space-y-2">
                <div className="bg-darkSurface p-2.5 rounded-lg border border-darkBorder text-gray-300">
                  <span className="font-semibold text-indigo-400">You: </span>
                  {chat.question}
                </div>
                <div className="bg-indigo-950/20 p-2.5 rounded-lg border border-indigo-900/30 text-gray-300">
                  <span className="font-semibold text-indigo-300">AI: </span>
                  {chat.answer}
                </div>
              </div>
            ))
          )}
          {chatLoading && (
            <div className="text-xs text-gray-500 italic animate-pulse">
              AI Advisor is reading document...
            </div>
          )}
        </div>

        {/* Chat Input form */}
        <form onSubmit={handleChat} className="flex gap-2">
          <input
            type="text"
            required
            disabled={doc.status !== 'COMPLETED' || chatLoading}
            placeholder="Type your question..."
            className="flex-1 px-3 py-2 rounded-lg glass-input text-xs"
            value={chatQuestion}
            onChange={(e) => setChatQuestion(e.target.value)}
          />
          <button
            type="submit"
            disabled={doc.status !== 'COMPLETED' || chatLoading}
            className="p-2.5 rounded-lg bg-brandPrimary hover:bg-indigo-700 text-white transition disabled:opacity-50"
          >
            <Send size={14} />
          </button>
        </form>
      </div>
    </div>
  );
}
