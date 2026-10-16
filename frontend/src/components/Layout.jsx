import React from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, FileUp, FolderOpen, GitCompare, MessageSquare, LogOut, FileText, User as UserIcon } from 'lucide-react';

export default function Layout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const username = localStorage.getItem('username') || 'User';
  const role = localStorage.getItem('role') || 'EMPLOYEE';

  const menuItems = [
    { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={20} /> },
    { name: 'Upload Document', path: '/upload', icon: <FileUp size={20} /> },
    { name: 'My Documents', path: '/documents', icon: <FolderOpen size={20} /> },
    { name: 'Compare Documents', path: '/compare', icon: <GitCompare size={20} /> }
  ];

  const handleSignOut = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="flex min-h-screen bg-darkBg text-gray-100">
      {/* Sidebar */}
      <aside className="w-64 glass-panel border-r border-darkBorder flex flex-col justify-between hidden md:flex">
        <div>
          {/* Logo */}
          <div className="p-6 border-b border-darkBorder flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-brandPrimary flex items-center justify-center text-white font-bold text-lg">
              D
            </div>
            <div>
              <h1 className="font-bold text-sm leading-tight text-white">Document Intel</h1>
              <span className="text-[10px] text-gray-500 font-medium tracking-wide">ENTERPRISE CORE</span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="p-4 space-y-1">
            {menuItems.map((item) => {
              const active = location.pathname === item.path;
              return (
                <Link
                  key={item.name}
                  to={item.path}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition duration-200 ${
                    active
                      ? 'bg-brandPrimary text-white shadow-lg shadow-indigo-500/20'
                      : 'text-gray-400 hover:bg-darkSurface hover:text-white'
                  }`}
                >
                  {item.icon}
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User profile & Sign Out */}
        <div className="p-4 border-t border-darkBorder space-y-4">
          <div className="flex items-center gap-3 px-2">
            <div className="h-9 w-9 rounded-full bg-indigo-900/50 border border-indigo-500/30 flex items-center justify-center text-indigo-300">
              <UserIcon size={18} />
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-semibold text-white truncate">{username}</p>
              <p className="text-[10px] text-gray-500 font-medium truncate uppercase">{role.replace('ROLE_', '')}</p>
            </div>
          </div>

          <button
            onClick={handleSignOut}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium text-red-400 hover:bg-red-950/20 transition duration-200"
          >
            <LogOut size={20} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col">
        {/* Header (Top Nav) */}
        <header className="h-16 border-b border-darkBorder flex items-center justify-between px-8 bg-darkBg/50 backdrop-blur-md sticky top-0 z-40">
          <div className="flex items-center gap-4">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-widest">
              {location.pathname === '/' ? 'Overview' : location.pathname.substring(1).replace('-', ' ')}
            </span>
          </div>
          <div className="flex items-center gap-4 text-xs font-medium text-gray-400">
            <span>Server Status: <span className="text-emerald-400">Active</span></span>
          </div>
        </header>

        {/* Body content */}
        <main className="flex-1 p-8 overflow-y-auto max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
