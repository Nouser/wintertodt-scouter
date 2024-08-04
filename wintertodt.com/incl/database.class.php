<?php
class Database {
	
	private $host;
	private $username;
	private $password;
	private $database;
	private $con;
	private $table;
	
	/**
	 * @param String $host
	 * @param String $user
	 * @param String $pass
	 * @param String $db
	 * @param String $table
	 */
	public function __construct($user, $pass, $db, $host, $table) {
		$this->host 	= $host;
		$this->username = $user;
		$this->password = $pass;
		$this->database = $db;
		$this->table 	= $table;
	}
	
	public function connect() {
		$this->con = new PDO('mysql:host=localhost;dbname='.$this->database.';charset=utf8', $this->username, $this->password);
		$this->con->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$this->con->setAttribute(PDO::ATTR_EMULATE_PREPARES, false);
	}
	
	public function getCon() {
		return $this->con;
	}
	
	public function get_results($stmt) {
		$stmt = $this->con->prepare($stmt);
		$stmt->execute();
		return $stmt->fetchAll(PDO::FETCH_ASSOC);
	}

    
    public function add_bossData($health, $world, $timer, $timestamp) {
        $stmt = $this->con->prepare("INSERT INTO wintertodt.serverData(health, world, timer, timestamp) VALUES(:health, :world, :timer, :timestamp)");
        $stmt->execute(array(
        "health" => $health,
        "world" => $world,
        "timer" => $timer,
        "timestamp" => $timestamp
        ));
    }
    
    public function updateWorldData($world, $health, $timer, $timestamp) {
        $stmt =  $this->con->prepare("UPDATE serverData SET health= :health, timer = :timer, timestamp = :timestamp WHERE world = :world");
        $stmt->execute(array(
        "health" => $health,
        "world" => $world,
        "timer" => $timer,
        "timestamp" => $timestamp
        ));
    }
    
    public function getAllData() {
		$stmt = "SELECT health, world, timer, timestamp FROM wintertodt.serverData";
		$stmt = $this->con->prepare($stmt);
		$stmt->execute();
		return $stmt->fetchAll(PDO::FETCH_ASSOC);
	}
	
	public function getAllData2() {
		$stmt = "SELECT health AS a, world AS b, timer AS d, timestamp AS c FROM wintertodt.serverData";
		$stmt = $this->con->prepare($stmt);
		$stmt->execute();
		return $stmt->fetchAll(PDO::FETCH_ASSOC);
	}
	
	public function getDataForWorld($world) {
		$stmt = "SELECT * FROM wintertodt.serverData WHERE world = :world";
		$stmt = $this->con->prepare($stmt);
		$stmt->execute(array(
	    "world"  => $world));
		return $stmt->fetchAll(PDO::FETCH_ASSOC);
	}
    
	
	public function mres($value) {
	    $search = array("\\",  "\x00", "\n",  "\r",  "'",  '"', "\x1a");
	    $replace = array("\\\\","\\0","\\n", "\\r", "\'", '\"', "\\Z");

	    return str_replace($search, $replace, $value);
	}
	//$stmt->bindParam(":amount", $min);
}
?>

