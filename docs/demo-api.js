// Bản demo cho GitHub Pages: thay máy chủ Java bằng dữ liệu lưu ngay trong trình duyệt.
// Quy tắc đăng ký giống service/RegistrationService.java.
const MAX_CREDITS = 18;

// Mật khẩu công khai chỉ để demo, kiểm tra ngay trong trình duyệt nên không phải bảo mật thật
const DEMO_PASSWORD = 'demo1234';

const STUDENTS = { SV01: 'Sinh viên 01', SV02: 'Sinh viên 02', SV03: 'Sinh viên 03', SV04: 'Sinh viên 04' };

// [mã, tên, tín chỉ, giảng viên, sĩ số tối đa, lịch, phòng]
const COURSES = [
  ['EN101', 'Tiếng Anh', 2, 'Phạm Dung', 1, 'Thứ 4 08:00–10:00', 'P.105'],
  ['IT101', 'Lập trình Java', 3, 'Nguyễn An', 20, 'Thứ 2 08:00–10:00', 'P.301'],
  ['IT102', 'Cơ sở dữ liệu', 3, 'Trần Bình', 20, 'Thứ 2 09:00–11:00', 'P.201'],
  ['IT103', 'Mạng máy tính', 3, 'Vũ Hà', 20, 'Thứ 5 08:00–10:00', 'P.401'],
  ['IT104', 'Trí tuệ nhân tạo', 3, 'Phạm Văn Dũng', 20, 'Thứ 4 13:00–15:30', 'P.402'],
  ['IT105', 'Cấu trúc dữ liệu và giải thuật', 3, 'Hoàng Mai', 20, 'Thứ 6 08:00–10:00', 'P.302'],
  ['IT106', 'Hệ điều hành', 3, 'Đỗ Quân', 20, 'Thứ 2 13:00–15:00', 'P.303'],
  ['IT107', 'Công nghệ phần mềm', 3, 'Bùi Lan', 20, 'Thứ 3 13:00–15:00', 'P.304'],
  ['MA101', 'Toán rời rạc', 3, 'Lê Chi', 20, 'Thứ 3 08:00–10:00', 'P.202'],
  ['MA102', 'Xác suất thống kê', 3, 'Ngô Hải', 20, 'Thứ 5 13:00–15:00', 'P.203'],
  ['PH101', 'Vật lý đại cương', 2, 'Đặng Sơn', 20, 'Thứ 7 08:00–10:00', 'P.106']
].map(([id, name, credits, lecturer, capacity, schedule, room]) =>
  ({ id, name, credits, lecturer, capacity, schedule, room }));

function readSaved() {
  try {
    return JSON.parse(localStorage.getItem('demo-registrations')) || {};
  } catch {
    return {};
  }
}

const registrations = readSaved(); // { SV01: ['IT101', ...] }

function save() {
  try {
    localStorage.setItem('demo-registrations', JSON.stringify(registrations));
  } catch {
    // Trình duyệt chặn lưu thì dữ liệu chỉ giữ đến khi tải lại trang
  }
}

const enrolled = id => Object.values(registrations).filter(list => list.includes(id)).length;

// Hai lịch trùng nhau khi cùng thứ và khoảng giờ giao nhau
function overlaps(a, b) {
  const parse = schedule => {
    const split = schedule.lastIndexOf(' ');
    return [schedule.slice(0, split), ...schedule.slice(split + 1).split('–')];
  };
  const [dayA, startA, endA] = parse(a);
  const [dayB, startB, endB] = parse(b);
  return dayA === dayB && startA < endB && startB < endA;
}

// Cùng tên và cách dùng với hàm gọi máy chủ trong bản thật
async function api(url, body) {
  if (url === '/api/login') {
    if (!STUDENTS[body.student] || body.password !== DEMO_PASSWORD) {
      throw new Error('Sai mã sinh viên hoặc mật khẩu.');
    }
    return { token: body.student, name: STUDENTS[body.student] };
  }
  if (url === '/api/logout') return {};

  if (!STUDENTS[token]) throw new Error('Vui lòng đăng nhập.');
  const mine = registrations[token] || [];
  const chosen = COURSES.filter(course => mine.includes(course.id));
  const credits = chosen.reduce((sum, course) => sum + course.credits, 0);

  if (url === '/api/state') {
    return {
      student: { id: token, name: STUDENTS[token], max_credits: MAX_CREDITS },
      credits,
      courses: COURSES.map(course => ({
        ...course, enrolled: enrolled(course.id), registered: mine.includes(course.id)
      }))
    };
  }

  const course = COURSES.find(item => item.id === body.course);
  if (!course) throw new Error('Không tìm thấy môn học.');

  if (body.action === 'drop') {
    if (!mine.includes(course.id)) throw new Error('Bạn chưa đăng ký môn này.');
    registrations[token] = mine.filter(id => id !== course.id);
    save();
    return { message: 'Hủy đăng ký thành công.' };
  }

  if (mine.includes(course.id)) throw new Error('Bạn đã đăng ký môn này.');
  if (enrolled(course.id) >= course.capacity) throw new Error('Môn học đã đủ số lượng.');
  if (credits + course.credits > MAX_CREDITS) throw new Error('Vượt giới hạn tín chỉ.');
  const clash = chosen.find(item => overlaps(course.schedule, item.schedule));
  if (clash) throw new Error(`Lịch học trùng với môn ${clash.id}.`);

  registrations[token] = [...mine, course.id];
  save();
  return { message: 'Đăng ký thành công.' };
}
