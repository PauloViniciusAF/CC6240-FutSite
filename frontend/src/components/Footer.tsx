import { Link } from 'react-router-dom';
import './Footer.css';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="footer-content">
        <a 
          href="https://github.com/CC6240-FutSite/futsite" 
          target="_blank" 
          rel="noopener noreferrer"
          className="footer-link"
        >
          Sobre
        </a>
        
        <span className="footer-divider">•</span>
        
        <span className="footer-version">v1.0.0 © {currentYear}</span>
        
        <span className="footer-divider">•</span>
        
        <Link to="/database" className="footer-link">
        Status
        </Link>
      </div>
    </footer>
  );
}
