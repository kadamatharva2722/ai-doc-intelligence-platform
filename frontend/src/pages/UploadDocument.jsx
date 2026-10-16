import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';
import { Upload, AlertCircle, FileText, CheckCircle2 } from 'lucide-react';

export default function UploadDocument() {
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (!selectedFile) return;

    const allowedExtensions = ['pdf', 'docx', 'txt'];
    const extension = selectedFile.name.split('.').pop().toLowerCase();

    if (!allowedExtensions.includes(extension)) {
      setError('Unsupported file type. Please upload a PDF, DOCX, or TXT file.');
      setFile(null);
      return;
    }

    if (selectedFile.size > 10 * 1024 * 1024) {
      setError('File size exceeds the 10MB limit.');
      setFile(null);
      return;
    }

    setError('');
    setFile(selectedFile);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a valid file to upload.');
      return;
    }

    setError('');
    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      await API.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setSuccess(true);
      setTimeout(() => {
        navigate('/documents');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'File upload failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-bold text-white">Upload Document</h2>
        <p className="text-sm text-gray-400">Add document iterations for automatic AI indexing and metadata extraction.</p>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-900/30 border border-red-500/50 text-red-200 text-sm">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-green-900/30 border border-green-500/50 text-green-200 text-sm">
          <CheckCircle2 size={18} />
          <span>Upload successful! Redirecting to documents file cabinet...</span>
        </div>
      )}

      <form onSubmit={handleUpload} className="p-8 rounded-xl glass-panel shadow-md space-y-6">
        <div className="border-2 border-dashed border-darkBorder hover:border-brandPrimary/50 rounded-xl p-8 flex flex-col items-center justify-center cursor-pointer transition">
          <input
            type="file"
            id="file-upload"
            className="hidden"
            onChange={handleFileChange}
            accept=".pdf,.docx,.txt"
          />
          <label htmlFor="file-upload" className="flex flex-col items-center cursor-pointer w-full text-center">
            <div className="p-4 rounded-full bg-indigo-950/40 text-indigo-400 mb-4 border border-indigo-500/10">
              <Upload size={32} />
            </div>
            {file ? (
              <div className="space-y-1">
                <p className="text-sm font-semibold text-white truncate max-w-sm">{file.name}</p>
                <p className="text-xs text-gray-400">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
              </div>
            ) : (
              <div className="space-y-1">
                <p className="text-sm font-medium text-gray-300">Click to select files or drag and drop here</p>
                <p className="text-xs text-gray-500">Supports PDF, DOCX, TXT up to 10MB</p>
              </div>
            )}
          </label>
        </div>

        <button
          type="submit"
          disabled={!file || loading}
          className="w-full py-3 rounded-lg bg-brandPrimary hover:bg-indigo-700 text-white font-medium text-sm transition disabled:opacity-50"
        >
          {loading ? 'Uploading and publishing event...' : 'Process Document'}
        </button>
      </form>
    </div>
  );
}
