import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';

const API_BASE = 'http://localhost:8080/api/library/books';

function App() {
  const [bookId, setBookId] = useState('1');
  const [books, setBooks] = useState([]);
  const [selectedBook, setSelectedBook] = useState(null);
  const [availability, setAvailability] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || `Request failed: ${res.status}`);
    }
    return res.json();
  }

  const listBooks = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchJson(`${API_BASE}`);
      setBooks(data);
      setSelectedBook(null);
      setAvailability(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getBookById = async () => {
    const id = Number(bookId);
    if (!id || id <= 0) {
      setError('Please enter a valid positive book ID.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const data = await fetchJson(`${API_BASE}/${id}`);
      setSelectedBook(data);
      setAvailability(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const checkAvailability = async () => {
    const id = Number(bookId);
    if (!id || id <= 0) {
      setError('Please enter a valid positive book ID.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const data = await fetchJson(`${API_BASE}/${id}/availability`);
      setAvailability(data);
      setSelectedBook(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <header className="header">
        <div>
          <p className="eyebrow">Microservice Demo</p>
          <h1>Book Library gRPC UI</h1>
        </div>
        <span className="badge">gRPC + REST</span>
      </header>

      <section className="panel concept-panel">
        <h2>gRPC Concept</h2>
        <p>
          This UI demonstrates a client-server gRPC flow. The React app talks to the
          <strong> LibraryService</strong>, which internally invokes the
          <strong> BookService</strong> over gRPC.
        </p>
        <ul>
          <li>BookService: gRPC server</li>
          <li>LibraryService: REST gateway + gRPC client</li>
          <li>React UI: testing interface</li>
        </ul>
      </section>

      <section className="panel controls">
        <label htmlFor="bookId">Book ID</label>
        <div className="action-row">
          <input
            id="bookId"
            type="number"
            min="1"
            value={bookId}
            onChange={(e) => setBookId(e.target.value)}
            placeholder="Enter a book ID"
          />
          <button onClick={getBookById} disabled={loading}>Get Book</button>
          <button className="secondary" onClick={checkAvailability} disabled={loading}>
            Check Availability
          </button>
        </div>

        <div className="action-row buttons-row">
          <button onClick={listBooks} disabled={loading}>List All Books</button>
        </div>
      </section>

      {error && <div className="panel error">{error}</div>}

      {loading && <div className="panel info">Loading...</div>}

      <div className="grid">
        <section className="panel">
          <h2>Books List</h2>
          {books.length === 0 ? (
            <p className="muted">No books loaded yet.</p>
          ) : (
            <ul className="book-list">
              {books.map((book) => (
                <li key={book.id}>
                  <strong>#{book.id}</strong> — {book.title}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel">
          <h2>Selected Result</h2>
          {selectedBook ? (
            <pre>{JSON.stringify(selectedBook, null, 2)}</pre>
          ) : availability ? (
            <pre>{JSON.stringify(availability, null, 2)}</pre>
          ) : (
            <p className="muted">No result yet.</p>
          )}
        </section>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
