import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  DatePicker,
  Select,
  Input,
  Row,
  Col,
  message,
  Tooltip,
  Badge,
  Dropdown,
  Menu,
  Modal,
  Spin
} from 'antd';
import {
  SearchOutlined,
  SyncOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  EyeOutlined,
  LinkOutlined,
  MoreOutlined
} from '@ant-design/icons';
import moment from 'moment';
import { useDispatch, useSelector } from 'react-redux';
import { receiptService } from '../../services/receiptService';
import ReceiptSearchForm from '../../components/receipt/ReceiptSearchForm';
import ReceiptDetailModal from '../../components/receipt/ReceiptDetailModal';
import ReceiptMatchModal from '../../components/receipt/ReceiptMatchModal';

const { RangePicker } = DatePicker;
const { Option } = Select;

const ReceiptManagementPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector(state => state.auth);
  
  const [receipts, setReceipts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [selectedReceipts, setSelectedReceipts] = useState([]);
  const [searchParams, setSearchParams] = useState({
    dateRange: [moment().subtract(1, 'month'), moment()],
    cardId: null,
    merchantName: '',
    minAmount: null,
    maxAmount: null,
    matchStatus: 'ALL',
    receiptType: 'ALL'
  });
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0
  });
  
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [matchModalVisible, setMatchModalVisible] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);

  useEffect(() => {
    fetchReceipts();
  }, [searchParams, pagination.current, pagination.pageSize]);

  const fetchReceipts = async () => {
    setLoading(true);
    try {
      const params = {
        ...searchParams,
        startDate: searchParams.dateRange[0].format('YYYY-MM-DD'),
        endDate: searchParams.dateRange[1].format('YYYY-MM-DD'),
        page: pagination.current - 1,
        size: pagination.pageSize
      };
      
      const response = await receiptService.getReceipts(params);
      setReceipts(response.content);
      setPagination({
        ...pagination,
        total: response.totalElements
      });
    } catch (error) {
      message.error('영수증 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await receiptService.syncReceipts();
      message.success('영수증 동기화가 완료되었습니다.');
      fetchReceipts();
    } catch (error) {
      message.error('영수증 동기화에 실패했습니다.');
    } finally {
      setSyncing(false);
    }
  };

  const handleViewDetail = (receipt) => {
    setSelectedReceipt(receipt);
    setDetailModalVisible(true);
  };

  const handleMatch = (receipt) => {
    setSelectedReceipt(receipt);
    setMatchModalVisible(true);
  };

  const handleExport = async (format) => {
    try {
      const blob = await receiptService.exportReceipts({
        receiptIds: selectedReceipts.length > 0 ? selectedReceipts : null,
        format: format,
        ...searchParams
      });
      
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `receipts_${moment().format('YYYYMMDD')}.${format.toLowerCase()}`;
      a.click();
      window.URL.revokeObjectURL(url);
      
      message.success('파일 다운로드가 완료되었습니다.');
    } catch (error) {
      message.error('파일 다운로드에 실패했습니다.');
    }
  };

  const getMatchStatusTag = (matchStatus) => {
    const statusConfig = {
      MATCHED: { color: 'success', text: '매칭완료' },
      PENDING: { color: 'warning', text: '검토중' },
      UNMATCHED: { color: 'default', text: '미매칭' }
    };
    const config = statusConfig[matchStatus] || statusConfig.UNMATCHED;
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const columns = [
    {
      title: '거래일시',
      dataIndex: 'transactionDate',
      key: 'transactionDate',
      width: 150,
      render: (date) => moment(date).format('YYYY-MM-DD HH:mm'),
      sorter: true
    },
    {
      title: '가맹점명',
      dataIndex: 'merchantName',
      key: 'merchantName',
      width: 200,
      ellipsis: true,
      render: (text) => (
        <Tooltip title={text}>
          <span>{text}</span>
        </Tooltip>
      )
    },
    {
      title: '금액',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      align: 'right',
      render: (amount) => `${amount.toLocaleString()}원`,
      sorter: true
    },
    {
      title: '카드',
      dataIndex: 'cardAlias',
      key: 'cardAlias',
      width: 120
    },
    {
      title: '영수증유형',
      dataIndex: 'receiptType',
      key: 'receiptType',
      width: 100,
      render: (type) => {
        const typeConfig = {
          CARD_SLIP: { text: '카드전표' },
          TAX_INVOICE: { text: '세금계산서' },
          CASH_RECEIPT: { text: '현금영수증' }
        };
        return typeConfig[type]?.text || type;
      }
    },
    {
      title: '매칭상태',
      dataIndex: 'matchStatus',
      key: 'matchStatus',
      width: 100,
      render: (status) => getMatchStatusTag(status)
    },
    {
      title: '검증',
      dataIndex: 'isVerified',
      key: 'isVerified',
      width: 80,
      align: 'center',
      render: (verified) => (
        <Badge 
          status={verified ? 'success' : 'default'} 
          text={verified ? '완료' : '대기'}
        />
      )
    },
    {
      title: '작업',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => {
        const menu = (
          <Menu>
            <Menu.Item 
              key="view" 
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              상세보기
            </Menu.Item>
            <Menu.Item 
              key="match" 
              icon={<LinkOutlined />}
              onClick={() => handleMatch(record)}
              disabled={record.matchStatus === 'MATCHED'}
            >
              매칭하기
            </Menu.Item>
            <Menu.Item 
              key="download" 
              icon={<DownloadOutlined />}
              onClick={() => receiptService.downloadReceipt(record.receiptId)}
            >
              다운로드
            </Menu.Item>
          </Menu>
        );
        
        return (
          <Dropdown overlay={menu} trigger={['click']}>
            <Button type="text" icon={<MoreOutlined />} />
          </Dropdown>
        );
      }
    }
  ];

  const rowSelection = {
    selectedRowKeys: selectedReceipts,
    onChange: (selectedRowKeys) => {
      setSelectedReceipts(selectedRowKeys);
    }
  };

  return (
    <div className="receipt-management-page">
      <Card className="page-header">
        <Row justify="space-between" align="middle">
          <Col>
            <h1>전자영수증 관리</h1>
          </Col>
          <Col>
            <Space>
              <Button 
                icon={<SyncOutlined spin={syncing} />} 
                onClick={handleSync}
                loading={syncing}
              >
                동기화
              </Button>
              <Dropdown
                overlay={
                  <Menu>
                    <Menu.Item 
                      key="excel" 
                      icon={<FileExcelOutlined />}
                      onClick={() => handleExport('EXCEL')}
                    >
                      Excel 다운로드
                    </Menu.Item>
                    <Menu.Item 
                      key="pdf" 
                      icon={<FilePdfOutlined />}
                      onClick={() => handleExport('PDF')}
                    >
                      PDF 다운로드
                    </Menu.Item>
                  </Menu>
                }
              >
                <Button icon={<DownloadOutlined />}>
                  내보내기
                </Button>
              </Dropdown>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card className="search-card">
        <ReceiptSearchForm 
          onSearch={setSearchParams}
          loading={loading}
        />
      </Card>

      <Card className="table-card">
        <Table
          rowKey="receiptId"
          columns={columns}
          dataSource={receipts}
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}건`,
            onChange: (page, pageSize) => {
              setPagination({
                ...pagination,
                current: page,
                pageSize
              });
            }
          }}
          rowSelection={rowSelection}
          scroll={{ x: 1200 }}
        />
      </Card>

      <ReceiptDetailModal
        visible={detailModalVisible}
        receipt={selectedReceipt}
        onClose={() => {
          setDetailModalVisible(false);
          setSelectedReceipt(null);
        }}
      />

      <ReceiptMatchModal
        visible={matchModalVisible}
        receipt={selectedReceipt}
        onClose={() => {
          setMatchModalVisible(false);
          setSelectedReceipt(null);
        }}
        onSuccess={() => {
          setMatchModalVisible(false);
          setSelectedReceipt(null);
          fetchReceipts();
          message.success('영수증 매칭이 완료되었습니다.');
        }}
      />
    </div>
  );
};

export default ReceiptManagementPage;